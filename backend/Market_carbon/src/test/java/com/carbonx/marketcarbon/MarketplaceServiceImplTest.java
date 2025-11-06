package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.request.CreditListingUpdateRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.impl.MarketplaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit Test hoàn chỉnh cho Luồng 4: Niêm yết Tín chỉ (MarketplaceServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceServiceImplTest {

	@Mock private CarbonCreditRepository carbonCreditRepository;
	@Mock private CompanyRepository companyRepository;
	@Mock private UserRepository userRepository;
	@Mock private MarketplaceListingRepository marketplaceListingRepository;
	@Mock private CreditBatchRepository creditBatchRepository;

	@Mock private SecurityContext securityContext;
	@Mock private Authentication authentication;

	@InjectMocks
	private MarketplaceServiceImpl marketplaceService;

	private User sellerUser;
	private Company sellerCompany;
	private CreditBatch batch;
	private CarbonCredit credit1, credit2;

	@BeforeEach
	void setUp() {
		sellerUser = User.builder().id(1L).email("seller@test.com").build();
		sellerCompany = Company.builder().id(10L).user(sellerUser).companyName("Seller Inc.").build();

		// (carbonCredit = available, listedAmount = listed)
		credit1 = CarbonCredit.builder()
				.id(101L).company(sellerCompany).project(Project.builder().id(100L).logo("logo.png").build())
				.status(CreditStatus.AVAILABLE).amount(new BigDecimal("100"))
				.carbonCredit(new BigDecimal("100")) // available
				.listedAmount(BigDecimal.ZERO)       // listed
				.build();
		credit2 = CarbonCredit.builder()
				.id(102L).company(sellerCompany).project(Project.builder().id(100L).logo("logo.png").build())
				.status(CreditStatus.AVAILABLE).amount(new BigDecimal("50"))
				.carbonCredit(new BigDecimal("50")) // available
				.listedAmount(BigDecimal.ZERO)      // listed
				.build();

		batch = CreditBatch.builder()
				.id(1L)
				.company(sellerCompany)
				.project(Project.builder().id(100L).title("EV Project").logo("logo.png").build())
				.batchCode("BATCH-001")
				.carbonCredit(List.of(credit1, credit2))
				.expiresAt(LocalDate.now().plusYears(1))
				.build();

		credit1.setBatch(batch);
		credit2.setBatch(batch);

		lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		lenient().when(authentication.getName()).thenReturn(sellerUser.getEmail());
		lenient().when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(sellerUser);
		lenient().when(companyRepository.findByUserId(sellerUser.getId())).thenReturn(Optional.of(sellerCompany));
	}

	@Test
	@DisplayName("[Luồng 4 - List] Niêm yết thành công từ Batch (trừ 2 credit)")
	void listCreditsForSale_Success_FromBatch() {
		// Arrange
		when(creditBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
		when(marketplaceListingRepository.findByCompanyIdAndCarbonCredit_Batch_IdAndStatus(anyLong(), anyLong(), any()))
				.thenReturn(Collections.emptyList());

		when(marketplaceListingRepository.save(any(MarketPlaceListing.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CreditListingRequest request = CreditListingRequest.builder()
				.batchId(1L)
				.quantity(new BigDecimal("120")) // List 120 (credit1 100, credit2 20)
				.pricePerCredit(new BigDecimal("15"))
				.build();

		ArgumentCaptor<List<CarbonCredit>> creditsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<MarketPlaceListing> listingCaptor = ArgumentCaptor.forClass(MarketPlaceListing.class);

		// Act
		MarketplaceListingResponse response = marketplaceService.listCreditsForSale(request);

		// Assert
		// 1. Verify 2 credits bị cập nhật
		verify(carbonCreditRepository).saveAll(creditsCaptor.capture());
		List<CarbonCredit> savedCredits = creditsCaptor.getValue();
		assertThat(savedCredits).hasSize(2);

		// Credit 1 (100 -> 0 available, 100 listed)
		CarbonCredit savedCredit1 = savedCredits.stream().filter(c -> c.getId().equals(101L)).findFirst().get();
		assertThat(savedCredit1.getCarbonCredit()).isEqualByComparingTo("0"); // available
		assertThat(savedCredit1.getListedAmount()).isEqualByComparingTo("100"); // listed
		assertThat(savedCredit1.getStatus()).isEqualTo(CreditStatus.LISTED);

		// Credit 2 (50 -> 30 available, 20 listed)
		CarbonCredit savedCredit2 = savedCredits.stream().filter(c -> c.getId().equals(102L)).findFirst().get();
		assertThat(savedCredit2.getCarbonCredit()).isEqualByComparingTo("30"); // available
		assertThat(savedCredit2.getListedAmount()).isEqualByComparingTo("20"); // listed
		assertThat(savedCredit2.getStatus()).isEqualTo(CreditStatus.LISTED);

		// 2. Verify Listing được tạo
		verify(marketplaceListingRepository).save(listingCaptor.capture());

		// 3. Verify Response (không bị NPE)
		assertThat(response).isNotNull();
		assertThat(response.getQuantity()).isEqualByComparingTo("120"); // Số lượng yêu cầu
		assertThat(response.getOriginalQuantity()).isEqualByComparingTo("120");
		assertThat(response.getBatchId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("[Luồng 4 - List] Niêm yết thành công từ CreditID (lần đầu)")
	void listCreditsForSale_Success_FromCreditID_NewListing() {
		// Arrange
		// (Sử dụng credit1: 100 available, 0 listed)
		when(carbonCreditRepository.findByIdAndCompanyId(101L, 10L)).thenReturn(Optional.of(credit1));
		when(marketplaceListingRepository.findByCompanyIdAndCarbonCreditIdAndStatus(10L, 101L, ListingStatus.AVAILABLE))
				.thenReturn(Collections.emptyList());

		when(marketplaceListingRepository.save(any(MarketPlaceListing.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CreditListingRequest request = CreditListingRequest.builder()
				.carbonCreditId(101L)
				.quantity(new BigDecimal("40"))
				.pricePerCredit(new BigDecimal("10"))
				.build();

		ArgumentCaptor<CarbonCredit> creditCaptor = ArgumentCaptor.forClass(CarbonCredit.class);
		ArgumentCaptor<MarketPlaceListing> listingCaptor = ArgumentCaptor.forClass(MarketPlaceListing.class);

		// Act
		MarketplaceListingResponse response = marketplaceService.listCreditsForSale(request);

		// Assert
		// 1. Verify Credit được cập nhật
		verify(carbonCreditRepository).save(creditCaptor.capture());
		CarbonCredit savedCredit = creditCaptor.getValue();
		assertThat(savedCredit.getCarbonCredit()).isEqualByComparingTo("60"); // available (100 - 40)
		assertThat(savedCredit.getListedAmount()).isEqualByComparingTo("40"); // listed (0 + 40)
		assertThat(savedCredit.getAmount()).isEqualByComparingTo("100"); // total
		assertThat(savedCredit.getStatus()).isEqualTo(CreditStatus.LISTED);

		// 2. Verify Listing MỚI được tạo
		verify(marketplaceListingRepository).save(listingCaptor.capture());
		assertThat(listingCaptor.getValue().getQuantity()).isEqualByComparingTo("40");
		assertThat(listingCaptor.getValue().getOriginalQuantity()).isEqualByComparingTo("40");

		// 3. Verify Response
		assertThat(response.getQuantity()).isEqualByComparingTo("40");
	}

	@Test
	@DisplayName("[Luồng 4 - List] Niêm yết thành công từ CreditID (ghép vào listing cũ)")
	void listCreditsForSale_Success_FromCreditID_MergeListing() {
		// Arrange
		// (Sử dụng credit1: 100 available, 0 listed)
		when(carbonCreditRepository.findByIdAndCompanyId(101L, 10L)).thenReturn(Optional.of(credit1));

		// Đã có listing cũ (list 20)
		MarketPlaceListing existingListing = MarketPlaceListing.builder()
				.id(50L).company(sellerCompany).carbonCredit(credit1)
				.quantity(new BigDecimal("20")).originalQuantity(new BigDecimal("20"))
				.status(ListingStatus.AVAILABLE)
				.build();
		when(marketplaceListingRepository.findByCompanyIdAndCarbonCreditIdAndStatus(10L, 101L, ListingStatus.AVAILABLE))
				.thenReturn(List.of(existingListing));

		when(marketplaceListingRepository.save(any(MarketPlaceListing.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CreditListingRequest request = CreditListingRequest.builder()
				.carbonCreditId(101L)
				.quantity(new BigDecimal("30")) // List thêm 30
				.pricePerCredit(new BigDecimal("12")) // Giá mới
				.build();

		ArgumentCaptor<MarketPlaceListing> listingCaptor = ArgumentCaptor.forClass(MarketPlaceListing.class);

		// Act
		MarketplaceListingResponse response = marketplaceService.listCreditsForSale(request);

		// Assert
		// 1. Verify Credit được cập nhật
		verify(carbonCreditRepository).save(any(CarbonCredit.class)); // (Chi tiết đã test ở test trước)

		// 2. Verify Listing CŨ được cập nhật
		verify(marketplaceListingRepository).save(listingCaptor.capture());
		MarketPlaceListing savedListing = listingCaptor.getValue();
		assertThat(savedListing.getId()).isEqualTo(50L); // Phải là listing cũ
		assertThat(savedListing.getQuantity()).isEqualByComparingTo("50"); // 20 + 30
		assertThat(savedListing.getOriginalQuantity()).isEqualByComparingTo("50"); // 20 + 30
		assertThat(savedListing.getPricePerCredit()).isEqualByComparingTo("12"); // Giá mới

		// 3. Verify Response
		assertThat(response.getQuantity()).isEqualByComparingTo("50");
	}


	@Test
	@DisplayName("[Luồng 4 - List] Thất bại khi niêm yết từ Batch không đủ số lượng")
	void listCreditsForSale_Fail_InsufficientInBatch() {
		// Arrange
		when(creditBatchRepository.findById(1L)).thenReturn(Optional.of(batch)); // Tổng available là 150

		CreditListingRequest request = CreditListingRequest.builder()
				.batchId(1L)
				.quantity(new BigDecimal("200")) // Yêu cầu 200
				.pricePerCredit(new BigDecimal("15"))
				.build();

		// Act & Assert
		assertThatThrownBy(() -> marketplaceService.listCreditsForSale(request))
				.isInstanceOf(AppException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.AMOUNT_IS_NOT_ENOUGH);
	}

	@Test
	@DisplayName("[Luồng 4 - List] Thất bại khi niêm yết từ CreditID không đủ số lượng")
	void listCreditsForSale_Fail_InsufficientInCreditID() {
		// Arrange
		// (Sử dụng credit1: 100 available, 0 listed)
		when(carbonCreditRepository.findByIdAndCompanyId(101L, 10L)).thenReturn(Optional.of(credit1));
		when(marketplaceListingRepository.findByCompanyIdAndCarbonCreditIdAndStatus(10L, 101L, ListingStatus.AVAILABLE))
				.thenReturn(Collections.emptyList());

		CreditListingRequest request = CreditListingRequest.builder()
				.carbonCreditId(101L)
				.quantity(new BigDecimal("200")) // Yêu cầu 200, credit chỉ có 100
				.pricePerCredit(new BigDecimal("10"))
				.build();

		// Act & Assert
		assertThatThrownBy(() -> marketplaceService.listCreditsForSale(request))
				.isInstanceOf(AppException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.AMOUNT_IS_NOT_ENOUGH);
	}

	@Test
	@DisplayName("[Luồng 4 - Cancel List] Hủy niêm yết thành công (hoàn trả credit)")
	void deleteListCredits_Success() {
		// Arrange
		// Credit đã bị list: 40 available, 60 listed
		credit1.setCarbonCredit(new BigDecimal("40"));
		credit1.setListedAmount(new BigDecimal("60"));
		credit1.setAmount(new BigDecimal("100"));
		credit1.setStatus(CreditStatus.LISTED);

		MarketPlaceListing listing = MarketPlaceListing.builder()
				.id(99L)
				.company(sellerCompany)
				.carbonCredit(credit1)
				.quantity(new BigDecimal("60")) // Đang list 60
				.status(ListingStatus.AVAILABLE)
				.build();

		when(marketplaceListingRepository.findById(99L)).thenReturn(Optional.of(listing));
		when(carbonCreditRepository.findByIdWithPessimisticLock(credit1.getId())).thenReturn(Optional.of(credit1));

		ArgumentCaptor<CarbonCredit> creditCaptor = ArgumentCaptor.forClass(CarbonCredit.class);

		// Act
		MarketplaceListingResponse response = marketplaceService.deleteListCredits(99L);

		// Assert
		// 1. Verify Credit được hoàn trả
		verify(carbonCreditRepository).save(creditCaptor.capture());
		CarbonCredit savedCredit = creditCaptor.getValue();

		assertThat(savedCredit.getCarbonCredit()).isEqualByComparingTo("100"); // available (40 + 60)
		assertThat(savedCredit.getListedAmount()).isEqualByComparingTo("0"); // listed (60 - 60)
		assertThat(savedCredit.getAmount()).isEqualByComparingTo("100"); // total
		assertThat(savedCredit.getStatus()).isEqualTo(CreditStatus.AVAILABLE);

		// 2. Verify Listing bị xóa
		verify(marketplaceListingRepository).delete(listing);

		// 3. Verify Response
		assertThat(response.getListingId()).isEqualTo(99L);
		assertThat(response.getQuantity()).isEqualByComparingTo("60");
	}

	@Test
	@DisplayName("[Luồng 4 - Cancel List] Thất bại khi hủy listing không phải của mình")
	void deleteListCredits_Fail_NotOwner() {
		// Arrange
		Company otherCompany = Company.builder().id(99L).build();
		MarketPlaceListing otherListing = MarketPlaceListing.builder()
				.id(99L)
				.company(otherCompany) // [Test Case] Listing của công ty khác
				.status(ListingStatus.AVAILABLE)
				.build();

		when(marketplaceListingRepository.findById(99L)).thenReturn(Optional.of(otherListing));

		// Act & Assert
		assertThatThrownBy(() -> marketplaceService.deleteListCredits(99L))
				.isInstanceOf(AppException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMPANY_NOT_OWN);
	}

	@Test
	@DisplayName("[Luồng 4 - Cancel List] Thất bại khi hủy listing đã SOLD")
	void deleteListCredits_Fail_NotAvailable() {
		// Arrange
		MarketPlaceListing soldListing = MarketPlaceListing.builder()
				.id(99L)
				.company(sellerCompany)
				.status(ListingStatus.SOLD) // [Test Case] Listing đã bán
				.build();

		when(marketplaceListingRepository.findById(99L)).thenReturn(Optional.of(soldListing));

		// Act & Assert
		assertThatThrownBy(() -> marketplaceService.deleteListCredits(99L))
				.isInstanceOf(AppException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LISTING_IS_NOT_AVAILABLE);
	}
}

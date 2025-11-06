package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.certificate.CertificateData;
import com.carbonx.marketcarbon.certificate.CertificatePdfService;
import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.StorageService;
import com.carbonx.marketcarbon.service.FileStorageService; // SỬA: Import FileStorageService
import com.carbonx.marketcarbon.service.credit.SerialNumberService;
import com.carbonx.marketcarbon.service.credit.formula.CreditComputationResult;
import com.carbonx.marketcarbon.service.credit.formula.CreditFormula;
import com.carbonx.marketcarbon.service.impl.CreditIssuanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test hoàn chỉnh cho Luồng 3: Cấp phát Tín chỉ (CreditIssuanceServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class CreditIssuanceServiceImplTest {

    @Mock private EmissionReportRepository reportRepo;
    @Mock private CreditBatchRepository batchRepo;
    @Mock private CarbonCreditRepository creditRepo;
    @Mock private CompanyRepository companyRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CreditCertificateRepository certificateRepo;
    @Mock private CertificatePdfService certificatePdfService;
    @Mock private EmailService emailService;
    @Mock private CreditFormula creditFormula;
    @Mock private SerialNumberService serialSvc;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private SseService sseService;

    // SỬA: Mock FileStorageService
    @Mock private FileStorageService fileStorageService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private CreditIssuanceServiceImpl creditIssuanceService;

    private EmissionReport adminApprovedReport;
    private Company company;
    private User companyUser;
    private Project project;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        companyUser = User.builder().id(1L).email("company@test.com").build();
        company = Company.builder().id(10L).user(companyUser).companyName("Test Company").build();
        project = Project.builder().id(100L).title("EV Project").build();
        wallet = Wallet.builder().id(50L).company(company).user(companyUser).balance(BigDecimal.ZERO).carbonCreditBalance(BigDecimal.ZERO).build();

        adminApprovedReport = EmissionReport.builder()
                .id(1L)
                .seller(company)
                .project(project)
                .status(EmissionStatus.ADMIN_APPROVED) // Phải được ADMIN_APPROVED
                .period("2025-10")
                .totalEnergy(new BigDecimal("250000"))
                .totalCo2(new BigDecimal("100000")) // 100 Tấn
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("[Luồng 3 - Issue] Cấp phát tín chỉ thành công (issueForReport)")
    void issueForReport_Success() throws Exception {
        // Arrange
        final int CREDIT_COUNT = 100;
        CreditComputationResult formulaResult = new CreditComputationResult(new BigDecimal("100.000"), CREDIT_COUNT, BigDecimal.ZERO);
        SerialNumberService.SerialRange serialRange = new SerialNumberService.SerialRange(1L, 100L);
        CreditCertificate savedCert = CreditCertificate.builder().id(1L).certificateCode("CERT-001").build();
        // SỬA: StoredObject đến từ StorageService (interface cha của FileStorageService)
        StorageService.StoredObject storedPdf = new StorageService.StoredObject("key", "etag", "http://pdf.url");

        // Mock các lệnh find
        when(reportRepo.findById(1L)).thenReturn(Optional.of(adminApprovedReport));
        when(batchRepo.findByReportId(1L)).thenReturn(Optional.empty());
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(creditFormula.compute(any(), any())).thenReturn(formulaResult);
        when(serialSvc.allocate(any(), any(), anyInt(), eq(CREDIT_COUNT))).thenReturn(serialRange);
        when(serialSvc.buildCode(anyInt(), anyString(), anyString(), anyLong())).thenReturn("CODE-000001");
        when(walletRepository.findByCompany(company)).thenReturn(Optional.of(wallet));
        when(certificateRepo.save(any(CreditCertificate.class))).thenReturn(savedCert);
        when(certificatePdfService.generateAndUploadPdf(any(CertificateData.class))).thenReturn(storedPdf);

        // SỬA LỖI NPE: Mock `save` trả về đối tượng đã được build (có issuedAt)
        when(batchRepo.save(any(CreditBatch.class))).thenAnswer(inv -> {
            CreditBatch batchToSave = inv.getArgument(0);
            batchToSave.setId(1L); // Giả lập DB gán ID
            return batchToSave;
        });

        // SỬA LỖI NETWORK: Mock `fileStorageService.getObject()`
        byte[] fakePdfBytes = "fake-pdf-content".getBytes();
        when(fileStorageService.getObject(eq(storedPdf.key()))).thenReturn(fakePdfBytes);

        // Mock các service void
        doNothing().when(emailService).sendEmailWithAttachment(anyString(), anyString(), anyString(), any(), anyString());
        doNothing().when(sseService).sendNotificationToUser(anyLong(), anyString());

        // Act
        CreditBatchResponse response = creditIssuanceService.issueForReport(1L);

        // Assert
        // 1. Verify Report status
        ArgumentCaptor<EmissionReport> reportCaptor = ArgumentCaptor.forClass(EmissionReport.class);
        verify(reportRepo).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(EmissionStatus.CREDIT_ISSUED);

        // 2. Verify CreditBatch (đã được gọi)
        verify(batchRepo, times(2)).save(any(CreditBatch.class)); // 1 lần tạo, 1 lần cập nhật cert

        // 3. Verify CarbonCredits (100 cái)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CarbonCredit>> creditsCaptor = ArgumentCaptor.forClass(List.class);
        verify(creditRepo, times(2)).saveAll(creditsCaptor.capture()); // 1 lần ISSUED, 1 lần AVAILABLE
        List<CarbonCredit> savedCredits = creditsCaptor.getValue(); // Lấy lần gọi cuối (AVAILABLE)
        assertThat(savedCredits).hasSize(CREDIT_COUNT);
        assertThat(savedCredits.get(0).getStatus()).isEqualTo(CreditStatus.AVAILABLE);
        assertThat(savedCredits.get(0).getAmount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(savedCredits.get(0).getCarbonCredit()).isEqualByComparingTo(BigDecimal.ONE); // Available = 1

        // 4. Verify Wallet update
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getCarbonCreditBalance()).isEqualByComparingTo(new BigDecimal(CREDIT_COUNT));

        // 5. Verify WalletTransaction
        verify(walletTransactionRepository).save(any(WalletTransaction.class));

        // 6. Verify Certificate
        verify(certificatePdfService).generateAndUploadPdf(any(CertificateData.class));
        verify(fileStorageService).getObject("key"); // Verify đã gọi mock storage

        // 7. Verify Email (kiểm tra byte[] đã được mock)
        verify(emailService).sendEmailWithAttachment(
                eq(companyUser.getEmail()),
                anyString(),
                anyString(),
                eq(fakePdfBytes), // Đảm bảo đúng byte[]
                anyString()
        );

        // 8. Verify SSE
        verify(sseService).sendNotificationToUser(eq(companyUser.getId()), anyString());

        // 9. Verify Response
        assertThat(response).isNotNull();
        assertThat(response.getCreditsCount()).isEqualTo(CREDIT_COUNT);
    }

    @Test
    @DisplayName("[Luồng 3 - Issue] Thất bại khi báo cáo chưa được Admin duyệt")
    void issueForReport_Fail_WhenReportNotAdminApproved() {
        // Arrange
        adminApprovedReport.setStatus(EmissionStatus.CVA_APPROVED); // [Test Case]
        when(reportRepo.findById(1L)).thenReturn(Optional.of(adminApprovedReport));

        // Act & Assert
        assertThatThrownBy(() -> creditIssuanceService.issueForReport(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_NOT_APPROVED);
    }

    @Test
    @DisplayName("[Luồng 3 - Issue] Thất bại khi báo cáo đã được cấp phát")
    void issueForReport_Fail_WhenAlreadyIssued() {
        // Arrange
        when(reportRepo.findById(1L)).thenReturn(Optional.of(adminApprovedReport));
        when(batchRepo.findByReportId(1L)).thenReturn(Optional.of(new CreditBatch())); // [Test Case] Batch exists

        // Act & Assert
        assertThatThrownBy(() -> creditIssuanceService.issueForReport(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDIT_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("[Luồng 3 - Issue] Thất bại khi công thức tính trả về 0 credit")
    void issueForReport_Fail_WhenZeroCreditsComputed() {
        // Arrange
        CreditComputationResult zeroResult = new CreditComputationResult(BigDecimal.ZERO, 0, BigDecimal.ZERO);
        when(reportRepo.findById(1L)).thenReturn(Optional.of(adminApprovedReport));
        when(batchRepo.findByReportId(1L)).thenReturn(Optional.empty());
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(creditFormula.compute(any(), any())).thenReturn(zeroResult); // [Test Case]

        // Act & Assert
        assertThatThrownBy(() -> creditIssuanceService.issueForReport(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDIT_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("[Luồng 3 - List/Get] Lấy danh sách Batches (listAllBatches)")
    void listAllBatches_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<CreditBatch> batchList = List.of(
                CreditBatch.builder().id(1L).batchCode("B1").company(company).project(project).build(),
                CreditBatch.builder().id(2L).batchCode("B2").company(company).project(project).build()
        );
        Page<CreditBatch> page = new PageImpl<>(batchList, pageable, 2);
        when(batchRepo.findAll(pageable)).thenReturn(page);

        // Act
        Page<CreditBatchResponse> response = creditIssuanceService.listAllBatches(pageable);

        // Assert
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getBatchCode()).isEqualTo("B1");
    }

    @Test
    @DisplayName("[Luồng 3 - List/Get] Lấy Batch theo ID (getBatchById)")
    void getBatchById_Success() {
        // Arrange
        CreditBatch batch = CreditBatch.builder().id(1L).batchCode("B1").company(company).project(project).build();
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));

        // Act
        CreditBatchResponse response = creditIssuanceService.getBatchById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBatchCode()).isEqualTo("B1");
        assertThat(response.getCompanyName()).isEqualTo(company.getCompanyName());
    }

    @Test
    @DisplayName("[Luồng 3 - List/Get] Lấy Batch theo ID thất bại (Not Found)")
    void getBatchById_Fail_NotFound() {
        // Arrange
        when(batchRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> creditIssuanceService.getBatchById(99L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDIT_BATCH_NOT_FOUND);
    }

    @Test
    @DisplayName("[Luồng 3 - Trade] Cấp phát tín chỉ giao dịch (issueTradeCredit)")
    void issueTradeCredit_Success() {
        // Arrange
        User buyerUser = User.builder().id(2L).email("buyer@test.com").build();
        Company buyerCompany = Company.builder().id(20L).user(buyerUser).companyName("Buyer Inc.").build();
        CarbonCredit sourceCredit = CarbonCredit.builder()
                .id(101L).company(company).project(project)
                .batch(CreditBatch.builder().id(1L).expiresAt(LocalDate.now().plusYears(1)).build())
                .name("Source Credit").tCo2e(BigDecimal.ONE)
                .vintageYear(2024)
                .build();

        BigDecimal quantityToBuy = new BigDecimal("10");
        String issuer = "system@test.com";

        // Mock SerialNumberService
        when(serialSvc.allocate(any(), any(), anyInt(), eq(1)))
                .thenReturn(new SerialNumberService.SerialRange(1L, 1L)); // Giả lập cấp 1 serial mỗi lần
        when(serialSvc.buildCode(anyInt(), anyString(), anyString(), anyLong()))
                .thenReturn("NEW-CODE-000001"); // Giả sử mã mới

        // Act
        creditIssuanceService.issueTradeCredit(sourceCredit, buyerCompany, quantityToBuy, BigDecimal.TEN, issuer);

        // Assert
        // Verify 10 credits mới được tạo
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CarbonCredit>> captor = ArgumentCaptor.forClass(List.class);
        verify(creditRepo).saveAll(captor.capture());

        List<CarbonCredit> newCredits = captor.getValue();
        assertThat(newCredits).hasSize(10);

        // Kiểm tra chi tiết credit đầu tiên
        CarbonCredit firstNewCredit = newCredits.get(0);
        assertThat(firstNewCredit.getCompany()).isEqualTo(buyerCompany);
        assertThat(firstNewCredit.getSourceCredit()).isEqualTo(sourceCredit);
        assertThat(firstNewCredit.getStatus()).isEqualTo(CreditStatus.TRADED);
        assertThat(firstNewCredit.getAmount()).isEqualByComparingTo(BigDecimal.ONE); // 1 credit
        assertThat(firstNewCredit.getCarbonCredit()).isEqualByComparingTo(BigDecimal.ONE); // 1 available
        assertThat(firstNewCredit.getCreditCode()).isEqualTo("NEW-CODE-000001");
        assertThat(firstNewCredit.getVintageYear()).isEqualTo(2024);
    }
}

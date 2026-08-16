package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.repository.OrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.CreditIssuanceService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B5: carbon credits are DISCRETE units — unique serial per credit, integer
 * issuance formula (DefaultCreditFormula rounds to whole credits). The invariant
 * "quantity is a whole number" is now enforced at both entry points (order creation,
 * listing creation) and defended at settlement (intValueExact).
 */
@ExtendWith(MockitoExtension.class)
class OrderQuantityWholeNumberTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletRepository walletRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private MarketplaceListingRepository marketplaceListingRepository;
    @Mock private CarbonCreditRepository carbonCreditRepository;
    @Mock private CreditIssuanceService creditIssuanceService;
    @Mock private EntityManager entityManager;
    @Mock private Authentication authentication;

    private OrderServiceImpl service;

    private final User buyer = User.builder().id(1L).email("buyer@x.com").build();
    private final Company buyerCompany = Company.builder().id(101L).user(buyer).companyName("Buyer").build();
    private MarketPlaceListing listing;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, userRepository, walletTransactionService,
                walletRepository, companyRepository, marketplaceListingRepository,
                carbonCreditRepository, creditIssuanceService, entityManager);

        CarbonCredit credit = CarbonCredit.builder()
                .id(301L).creditCode("C-301").status(com.carbonx.marketcarbon.common.CreditStatus.AVAILABLE)
                .amount(new BigDecimal("10")).listedAmount(new BigDecimal("10"))
                .company(Company.builder().id(102L).companyName("Seller").build())
                .build();
        listing = MarketPlaceListing.builder()
                .id(201L).company(credit.getCompany()).carbonCredit(credit)
                .quantity(new BigDecimal("10")).soldQuantity(BigDecimal.ZERO)
                .pricePerCredit(new BigDecimal("2.00"))
                .status(ListingStatus.AVAILABLE)
                .expiresAt(java.time.LocalDate.now().plusDays(30))
                .build();

        when(authentication.getName()).thenReturn(buyer.getEmail());
        lenient().when(userRepository.findByEmail(buyer.getEmail())).thenReturn(buyer);
        lenient().when(companyRepository.findByUserId(buyer.getId())).thenReturn(Optional.of(buyerCompany));
        lenient().when(marketplaceListingRepository.findByIdWithDetails(201L)).thenReturn(Optional.of(listing));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private OrderRequest buyRequest(String quantity) {
        return OrderRequest.builder().buyerCompanyId(101L).listingId(201L)
                .quantity(new BigDecimal(quantity)).build();
    }

    @Test
    void createOrder_fractionalQuantity_isRejectedWithClearError() {
        assertThatThrownBy(() -> service.createOrder(buyRequest("2.5")))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.QUANTITY_MUST_BE_WHOLE));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_integerQuantityWithTrailingZeros_isAccepted() {
        // "3.000" is a whole number — stripTrailingZeros must let it through
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createOrder(buyRequest("3.000"));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualByComparingTo("3");
        assertThat(saved.getValue().getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_zeroOrNegative_keepsExistingValidation() {
        assertThatThrownBy(() -> service.createOrder(buyRequest("0")))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AMOUNT_IS_NOT_VALID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void settlement_fractionalQuantityDefense_failsLoudInsteadOfTruncating() {
        // A fractional order that somehow bypassed creation-time validation must NOT
        // silently truncate at settlement (buyer would pay for 2.5 but receive 2).
        // intValueExact() throws → transaction rolls back → controller marks ERROR (B4).
        Order order = Order.builder()
                .id(401L).company(buyerCompany).marketplaceListing(listing).carbonCredit(listing.getCarbonCredit())
                .orderStatus(OrderStatus.PENDING)
                .quantity(new BigDecimal("2.5"))
                .unitPrice(new BigDecimal("2.00")).totalPrice(new BigDecimal("5.00"))
                .platformFee(new BigDecimal("0.25")).sellerPayout(new BigDecimal("5.00"))
                .build();
        when(orderRepository.findByIdWithPessimisticLockAndDetails(401L)).thenReturn(Optional.of(order));
        when(marketplaceListingRepository.findByIdWithPessimisticLockAndDetails(201L))
                .thenReturn(Optional.of(listing));
        when(carbonCreditRepository.findByIdWithPessimisticLock(301L))
                .thenReturn(Optional.of(listing.getCarbonCredit()));

        com.carbonx.marketcarbon.model.Wallet buyerWallet = new com.carbonx.marketcarbon.model.Wallet();
        buyerWallet.setId(1L); buyerWallet.setUser(buyer);
        buyerWallet.setBalance(new BigDecimal("100.00")); buyerWallet.setCarbonCreditBalance(BigDecimal.ZERO);
        com.carbonx.marketcarbon.model.Wallet sellerWallet = new com.carbonx.marketcarbon.model.Wallet();
        sellerWallet.setId(2L); sellerWallet.setBalance(new BigDecimal("0.00"));
        sellerWallet.setCarbonCreditBalance(new BigDecimal("10"));
        when(walletRepository.findByCompanyIdWithDetails(101L)).thenReturn(buyerWallet);
        when(walletRepository.findByCompanyIdWithDetails(102L)).thenReturn(sellerWallet);

        assertThatThrownBy(() -> service.completeOrder(401L))
                .isInstanceOf(ArithmeticException.class);

        // nothing persisted as SUCCESS; no half-settlement ledger rows
        verify(walletTransactionService, never()).createTransaction(any());
    }
}

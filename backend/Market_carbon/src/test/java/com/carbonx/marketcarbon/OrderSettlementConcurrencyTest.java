package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
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
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B3: settlement concurrency guards.
 * Invariants under test:
 *  - the order row is loaded WITH a pessimistic lock (idempotency check under lock)
 *  - both settlement wallets are locked in ASCENDING id order (deadlock prevention)
 *  - an already-SUCCESS order never settles again
 *  - ledger rows carry the exact debit/credit amounts
 */
@ExtendWith(MockitoExtension.class)
class OrderSettlementConcurrencyTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletRepository walletRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private MarketplaceListingRepository marketplaceListingRepository;
    @Mock private CarbonCreditRepository carbonCreditRepository;
    @Mock private CreditIssuanceService creditIssuanceService;
    @Mock private EntityManager entityManager;

    private OrderServiceImpl service;

    private Company buyerCompany;
    private Company sellerCompany;
    private MarketPlaceListing listing;
    private CarbonCredit sourceCredit;
    private Order order;
    private Wallet buyerWallet;
    private Wallet sellerWallet;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, userRepository, walletTransactionService,
                walletRepository, companyRepository, marketplaceListingRepository,
                carbonCreditRepository, creditIssuanceService, entityManager);

        User buyerUser = User.builder().id(1L).email("buyer@x.com").build();
        User sellerUser = User.builder().id(2L).email("seller@x.com").build();
        buyerCompany = Company.builder().id(101L).user(buyerUser).companyName("Buyer").build();
        sellerCompany = Company.builder().id(102L).user(sellerUser).companyName("Seller").build();

        sourceCredit = CarbonCredit.builder()
                .id(301L).creditCode("C-301").status(CreditStatus.AVAILABLE)
                .amount(new BigDecimal("10")).listedAmount(new BigDecimal("10"))
                .carbonCredit(new BigDecimal("10")).company(sellerCompany)
                .build();

        listing = MarketPlaceListing.builder()
                .id(201L).company(sellerCompany).carbonCredit(sourceCredit)
                .quantity(new BigDecimal("5")).soldQuantity(BigDecimal.ZERO)
                .originalQuantity(new BigDecimal("5"))
                .pricePerCredit(new BigDecimal("2.00"))
                .status(ListingStatus.AVAILABLE)
                .expiresAt(java.time.LocalDate.now().plusDays(30))
                .build();

        order = Order.builder()
                .id(401L).company(buyerCompany).marketplaceListing(listing).carbonCredit(sourceCredit)
                .orderStatus(com.carbonx.marketcarbon.common.OrderStatus.PENDING)
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("2.00"))
                .totalPrice(new BigDecimal("6.00"))
                .platformFee(new BigDecimal("0.30"))
                .sellerPayout(new BigDecimal("6.00"))
                .createdAt(LocalDateTime.now())
                .build();

        // NOTE: buyer wallet id > seller wallet id — forces the ascending-order lock path
        buyerWallet = new Wallet();
        buyerWallet.setId(909L);
        buyerWallet.setUser(buyerUser);
        buyerWallet.setBalance(new BigDecimal("100.00"));
        buyerWallet.setCarbonCreditBalance(BigDecimal.ZERO);

        sellerWallet = new Wallet();
        sellerWallet.setId(808L);
        sellerWallet.setUser(sellerUser);
        sellerWallet.setBalance(new BigDecimal("50.00"));
        sellerWallet.setCarbonCreditBalance(new BigDecimal("10"));
    }

    private void stubHappyPath(Order orderState) {
        when(orderRepository.findByIdWithPessimisticLockAndDetails(401L)).thenReturn(Optional.of(orderState));
        when(marketplaceListingRepository.findByIdWithPessimisticLockAndDetails(201L)).thenReturn(Optional.of(listing));
        when(carbonCreditRepository.findByIdWithPessimisticLock(301L)).thenReturn(Optional.of(sourceCredit));
        when(walletRepository.findByCompanyIdWithDetails(101L)).thenReturn(buyerWallet);
        when(walletRepository.findByCompanyIdWithDetails(102L)).thenReturn(sellerWallet);
    }

    @Test
    void settlement_locksOrderRowWithPessimisticLock() {
        stubHappyPath(order);

        service.completeOrder(401L);

        verify(orderRepository).findByIdWithPessimisticLockAndDetails(401L);
        verify(orderRepository, never()).findByIdWithDetails(401L);
    }

    @Test
    void settlement_alreadySuccess_isIdempotentNoOp() {
        order.setOrderStatus(OrderStatus.SUCCESS);
        when(orderRepository.findByIdWithPessimisticLockAndDetails(401L)).thenReturn(Optional.of(order));

        service.completeOrder(401L);

        verify(marketplaceListingRepository, never()).findByIdWithPessimisticLockAndDetails(any());
        verify(walletTransactionService, never()).createTransaction(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void settlement_locksBothWalletsInAscendingIdOrder() {
        stubHappyPath(order);

        service.completeOrder(401L);

        InOrder lockOrder = inOrder(entityManager);
        // sellerWallet (808) must be locked BEFORE buyerWallet (909)
        lockOrder.verify(entityManager).find(Wallet.class, 808L, LockModeType.PESSIMISTIC_WRITE);
        lockOrder.verify(entityManager).find(Wallet.class, 909L, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void settlement_happyPath_movesMoneyCreditsAndStatusExactly() {
        stubHappyPath(order);

        service.completeOrder(401L);

        // order succeeded; listing decremented
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(listing.getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(listing.getSoldQuantity()).isEqualByComparingTo(new BigDecimal("3"));

        // wallet carbon balances: buyer +3, seller -3
        assertThat(buyerWallet.getCarbonCreditBalance()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(sellerWallet.getCarbonCreditBalance()).isEqualByComparingTo(new BigDecimal("7"));

        // one debit (BUY) and one credit (SELL) ledger row with exact amounts
        ArgumentCaptor<WalletTransactionRequest> ledger = ArgumentCaptor.forClass(WalletTransactionRequest.class);
        verify(walletTransactionService, times(2)).createTransaction(ledger.capture());
        List<WalletTransactionRequest> rows = ledger.getAllValues();
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.getType()).isEqualTo(WalletTransactionType.BUY_CARBON_CREDIT);
            assertThat(r.getAmount()).isEqualByComparingTo("6.00");
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.getType()).isEqualTo(WalletTransactionType.SELL_CARBON_CREDIT);
            assertThat(r.getAmount()).isEqualByComparingTo("6.00");
        });

        verify(creditIssuanceService).issueTradeCredit(any(), any(), any(), any(), any());
    }
}

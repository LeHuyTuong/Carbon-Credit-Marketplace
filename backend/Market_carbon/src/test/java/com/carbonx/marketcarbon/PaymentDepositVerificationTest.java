package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.PaymentVerificationService;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B1: the frontend can no longer declare a payment successful.
 * Invariants under test:
 *  - a deposit is credited only to the order's owner
 *  - a verified deposit is credited EXACTLY ONCE (status guard under lock)
 *  - every credit produces one ADD_MONEY ledger row with the right amount
 *  - VNPay credits convert VND order amounts to USD before touching the wallet
 */
@ExtendWith(MockitoExtension.class)
class PaymentDepositVerificationTest {

    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private SseService sseService;
    @Mock private PaymentVerificationService verificationService;
    @Mock private WalletService walletService;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private Authentication authentication;

    private PaymentServiceImpl service;

    private final User owner = User.builder().id(7L).email("owner@x.com").build();
    private final User attacker = User.builder().id(8L).email("attacker@x.com").build();
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(paymentOrderRepository, walletRepository, userRepository,
                sseService, verificationService, walletService, walletTransactionService);
        ReflectionTestUtils.setField(service, "stripeKey", "sk_test_x");
        ReflectionTestUtils.setField(service, "paypalClientId", "cid");
        ReflectionTestUtils.setField(service, "paypalClientSecret", "cs");
        ReflectionTestUtils.setField(service, "paypalMode", "sandbox");
        wallet = new Wallet();
        wallet.setId(9L);
        wallet.setUser(owner);
        wallet.setBalance(new BigDecimal("100.00"));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(User user) {
        when(authentication.getName()).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private PaymentOrder order(PaymentMethod method, Status status, String providerRef, Long amount, User user) {
        return PaymentOrder.builder()
                .id(1L).amount(amount).status(status).paymentMethod(method)
                .providerRef(providerRef).user(user)
                .build();
    }

    // ---------- Step 1: assertDepositVerifiable (ownership + provider proof) ----------

    @Test
    void deposit_orderOwnedByAnotherUser_isRejected() {
        loginAs(attacker);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.STRIPE, Status.PENDING, "cs_test_1", 10L, owner)));

        AppException ex = catchThrowableOfType(() -> service.assertDepositVerifiable(1L), AppException.class);
        assertThat(ex).isNotNull();
        verify(verificationService, never()).verifyStripePaid(anyString(), anyLong());
    }

    @Test
    void deposit_alreadySucceeded_isIdempotentFastPath() {
        loginAs(owner);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.STRIPE, Status.SUCCEEDED, "cs_test_1", 10L, owner)));

        // must not throw and must not call the provider again
        service.assertDepositVerifiable(1L);
        verify(verificationService, never()).verifyStripePaid(anyString(), anyLong());
    }

    @Test
    void deposit_providerSaysUnpaid_isRejectedAndNothingCredited() {
        loginAs(owner);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.STRIPE, Status.PENDING, "cs_test_1", 10L, owner)));
        when(verificationService.verifyStripePaid("cs_test_1", 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.assertDepositVerifiable(1L)).isInstanceOf(BadRequestException.class);
        // nothing should reach the credit step
        assertThatThrownBy(() -> service.assertDepositVerifiable(1L)).isInstanceOf(BadRequestException.class);
        verify(walletTransactionService, never()).createTransaction(any());
    }

    @Test
    void deposit_missingProviderRef_isRejected() {
        loginAs(owner);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.PAYPAL, Status.PENDING, null, 10L, owner)));

        assertThatThrownBy(() -> service.assertDepositVerifiable(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deposit_vnpayMethodViaClientEndpoint_isRejected() {
        loginAs(owner);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.VNPAY, Status.PENDING, null, 260000L, owner)));

        // VNPay must go through the signature-verified return flow, never this endpoint
        assertThatThrownBy(() -> service.assertDepositVerifiable(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deposit_verifiedStripe_passesAssertion() {
        loginAs(owner);
        when(paymentOrderRepository.findById(1L))
                .thenReturn(Optional.of(order(PaymentMethod.STRIPE, Status.PENDING, "cs_test_1", 10L, owner)));
        when(verificationService.verifyStripePaid("cs_test_1", 10L)).thenReturn(true);

        service.assertDepositVerifiable(1L); // no exception
    }

    // ---------- Step 2: applyVerifiedDeposit (exactly-once credit in one tx) ----------

    @Test
    void credit_pendingStripeOrder_creditsOnceWithLedgerRow() {
        PaymentOrder po = order(PaymentMethod.STRIPE, Status.PENDING, "cs_test_1", 10L, owner);
        when(paymentOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(po));
        when(walletRepository.findByUserId(7L)).thenReturn(wallet);

        Wallet result = service.applyVerifiedDeposit(1L);

        assertThat(po.getStatus()).isEqualTo(Status.SUCCEEDED);
        ArgumentCaptor<WalletTransactionRequest> ledger = ArgumentCaptor.forClass(WalletTransactionRequest.class);
        verify(walletTransactionService).createTransaction(ledger.capture());
        assertThat(ledger.getValue().getType()).isEqualTo(WalletTransactionType.ADD_MONEY);
        assertThat(ledger.getValue().getAmount()).isEqualByComparingTo(BigDecimal.TEN); // 10 USD
        assertThat(result).isSameAs(wallet);
    }

    @Test
    void credit_replayedCallAfterSucceeded_doesNotCreditAgain() {
        PaymentOrder po = order(PaymentMethod.STRIPE, Status.SUCCEEDED, "cs_test_1", 10L, owner);
        when(paymentOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(po));
        when(walletRepository.findByUserId(7L)).thenReturn(wallet);

        Wallet result = service.applyVerifiedDeposit(1L);

        assertThat(po.getStatus()).isEqualTo(Status.SUCCEEDED); // unchanged
        verify(walletTransactionService, never()).createTransaction(any());
        assertThat(result).isSameAs(wallet);
    }

    @Test
    void credit_terminalFailedOrder_doesNotCredit() {
        PaymentOrder po = order(PaymentMethod.PAYPAL, Status.FAILED, "PAY-1", 10L, owner);
        when(paymentOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(po));

        service.applyVerifiedDeposit(1L);

        verify(walletTransactionService, never()).createTransaction(any());
    }

    @Test
    void credit_userWithoutWallet_provisionsWalletThenCredits() {
        PaymentOrder po = order(PaymentMethod.STRIPE, Status.PENDING, "cs_test_1", 10L, owner);
        when(paymentOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(po));
        when(walletRepository.findByUserId(7L)).thenReturn(null).thenReturn(wallet);
        when(walletService.generateWallet(owner)).thenReturn(wallet);

        service.applyVerifiedDeposit(1L);

        verify(walletService).generateWallet(owner);
        verify(walletTransactionService).createTransaction(any());
    }

    // ---------- VNPay return path (HMAC-verified server-side) ----------

    @Test
    void vnpayDeposit_convertsVndToUsdBeforeCrediting() {
        PaymentOrder po = PaymentOrder.builder()
                .id(2L).amount(260000L).status(Status.PENDING).paymentMethod(PaymentMethod.VNPAY)
                .vnpTxnRef("TX123").user(owner)
                .build();
        when(paymentOrderRepository.findByVnpTxnRefWithLock("TX123")).thenReturn(Optional.of(po));
        when(walletRepository.findByUserId(7L)).thenReturn(wallet);

        boolean applied = service.applyVnPayDeposit("TX123");

        assertThat(applied).isTrue();
        assertThat(po.getStatus()).isEqualTo(Status.SUCCEEDED);
        ArgumentCaptor<WalletTransactionRequest> ledger = ArgumentCaptor.forClass(WalletTransactionRequest.class);
        verify(walletTransactionService).createTransaction(ledger.capture());
        // 260,000 VND at the 26,000 rate must be credited as 10 USD — NOT 260,000 USD
        assertThat(ledger.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void vnpayDeposit_unknownTxnRef_returnsFalseAndCreditsNothing() {
        when(paymentOrderRepository.findByVnpTxnRefWithLock("NOPE")).thenReturn(Optional.empty());

        assertThat(service.applyVnPayDeposit("NOPE")).isFalse();
        verify(walletTransactionService, never()).createTransaction(any());
    }

    @Test
    void vnpayDeposit_replayedReturn_doesNotCreditTwice() {
        PaymentOrder po = PaymentOrder.builder()
                .id(2L).amount(260000L).status(Status.SUCCEEDED).paymentMethod(PaymentMethod.VNPAY)
                .vnpTxnRef("TX123").user(owner)
                .build();
        when(paymentOrderRepository.findByVnpTxnRefWithLock("TX123")).thenReturn(Optional.of(po));

        assertThat(service.applyVnPayDeposit("TX123")).isFalse();
        verify(walletTransactionService, never()).createTransaction(any());
    }
}

package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.WithdrawalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-A (N3): withdrawal processing must be a guarded state transition.
 * Only a PENDING withdrawal may be accepted or rejected; the rejection refund
 * must go through the ledger (WalletTransactionService), exactly once.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalProcessReplayTest {

    @Mock private UserRepository userRepository;
    @Mock private WithdrawalRepository withdrawalRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ApplicationNotificationService applicationNotificationService;
    @Mock private SseService sseService;
    @Mock private WalletTransactionService walletTransactionService;

    private WithdrawalServiceImpl service;

    private final User user = User.builder().id(5L).email("u@example.com").build();
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new WithdrawalServiceImpl(userRepository, withdrawalRepository, walletRepository,
                applicationNotificationService, sseService, walletTransactionService);
        wallet = new Wallet();
        wallet.setId(9L);
        wallet.setBalance(new BigDecimal("100.00"));
    }

    private void stubSaveReturnsArgument() {
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Withdrawal withdrawalWithStatus(Status status) {
        return Withdrawal.builder()
                .id(1L)
                .amount(BigDecimal.TEN)
                .status(status)
                .user(user)
                .build();
    }

    @Test
    void rejectPending_refundsExactlyOnceThroughLedger() throws Exception {
        stubSaveReturnsArgument();
        when(withdrawalRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(withdrawalWithStatus(Status.PENDING)));
        when(walletRepository.findByUserId(5L)).thenReturn(wallet);

        Withdrawal result = service.processWithdrawal(1L, false);

        assertThat(result.getStatus()).isEqualTo(Status.REJECTED);
        ArgumentCaptor<WalletTransactionRequest> captor = ArgumentCaptor.forClass(WalletTransactionRequest.class);
        verify(walletTransactionService).createTransaction(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransactionType.WITHDRAWAL_REFUND);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        // no bare balance mutation path
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void rejectAlreadyRejected_throwsAndDoesNotRefundAgain() throws Exception {
        when(withdrawalRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(withdrawalWithStatus(Status.REJECTED)));

        assertThatThrownBy(() -> service.processWithdrawal(1L, false))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(walletTransactionService, never()).createTransaction(any());
    }

    @Test
    void acceptAfterRejected_throwsInsteadOfFlippingStatus() throws Exception {
        when(withdrawalRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(withdrawalWithStatus(Status.REJECTED)));

        assertThatThrownBy(() -> service.processWithdrawal(1L, true))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void acceptPending_succeedsWithoutRefund() throws Exception {
        stubSaveReturnsArgument();
        when(withdrawalRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(withdrawalWithStatus(Status.PENDING)));
        when(walletRepository.findByUserId(5L)).thenReturn(wallet);

        Withdrawal result = service.processWithdrawal(1L, true);

        assertThat(result.getStatus()).isEqualTo(Status.SUCCEEDED);
        verify(walletTransactionService, never()).createTransaction(any());
    }

    @Test
    void acceptTwice_secondCallThrows() throws Exception {
        when(withdrawalRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(withdrawalWithStatus(Status.SUCCEEDED)));

        assertThatThrownBy(() -> service.processWithdrawal(1L, true))
                .isInstanceOf(AppException.class);
        verify(walletTransactionService, never()).createTransaction(any());
    }
}

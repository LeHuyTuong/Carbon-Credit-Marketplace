package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.WalletServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B2: ledger correctness for wallet transfers.
 * Invariant: each ledger row's balanceBefore/balanceAfter must reflect the ACTUAL
 * wallet it belongs to — sender row shows X → X-amount, receiver row shows Y → Y+amount.
 * The receiver row previously copied the sender's values (copy-paste), corrupting
 * the audit trail of every profit-sharing payout.
 */
@ExtendWith(MockitoExtension.class)
class WalletTransferFundsLedgerTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private CarbonCreditRepository carbonCreditRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EntityManager entityManager;

    private WalletServiceImpl service;

    private Wallet from;
    private Wallet to;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(walletRepository, userRepository, walletTransactionService,
                walletTransactionRepository, carbonCreditRepository, companyRepository, entityManager);

        User sender = User.builder().id(1L).email("s@x.com").build();
        User receiver = User.builder().id(2L).email("r@x.com").build();

        from = new Wallet();
        from.setId(11L);
        from.setUser(sender);
        from.setBalance(new BigDecimal("100.00"));

        to = new Wallet();
        to.setId(22L);
        to.setUser(receiver);
        to.setBalance(new BigDecimal("50.00"));
    }

    private void stubLocks() {
        when(entityManager.find(Wallet.class, 11L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(from);
        when(entityManager.find(Wallet.class, 22L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(to);
    }

    @Test
    void transfer_recordsEachSideWithItsOwnBalances() throws Exception {
        stubLocks();

        service.transferFunds(from, to, new BigDecimal("30.00"),
                "PROFIT_SHARING", "debit desc", "credit desc", null);

        // balances mutated correctly
        assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("80.00"));

        // two ledger rows with correct per-side before/after
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<WalletTransaction> rows = captor.getAllValues();

        WalletTransaction debitRow = rows.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0).findFirst().orElseThrow();
        WalletTransaction creditRow = rows.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0).findFirst().orElseThrow();

        assertThat(debitRow.getWallet()).isSameAs(from);
        assertThat(debitRow.getBalanceBefore()).isEqualByComparingTo("100.00");
        assertThat(debitRow.getBalanceAfter()).isEqualByComparingTo("70.00");

        // the B2 bug: this row used to carry the SENDER's before/after
        assertThat(creditRow.getWallet()).isSameAs(to);
        assertThat(creditRow.getBalanceBefore()).isEqualByComparingTo("50.00");
        assertThat(creditRow.getBalanceAfter()).isEqualByComparingTo("80.00");
    }

    @Test
    void transfer_insufficientFunds_throwsAndWritesNoLedger() throws Exception {
        stubLocks();

        assertThatThrownBy(() -> service.transferFunds(from, to, new BigDecimal("150.00"),
                "PROFIT_SHARING", "d", "c", null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.WALLET_INSUFFICIENT_FUNDS));

        verify(walletTransactionRepository, never()).save(any());
        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        assertThat(to.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void transfer_nonPositiveAmount_throwsBeforeAnyMutation() {
        assertThatThrownBy(() -> service.transferFunds(from, to, BigDecimal.ZERO,
                "PROFIT_SHARING", "d", "c", null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MONEY_MUST_POSITIVE));
        verify(walletTransactionRepository, never()).save(any());
    }
}

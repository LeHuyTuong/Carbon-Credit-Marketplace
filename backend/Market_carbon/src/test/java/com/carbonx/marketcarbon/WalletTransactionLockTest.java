package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.impl.WalletTransactionServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B3: createTransaction is the chokepoint for balance mutations —
 * it must take the wallet row lock (SELECT ... FOR UPDATE) so that concurrent
 * mutations (deposit vs settlement vs withdrawal) serialize instead of losing updates.
 */
@ExtendWith(MockitoExtension.class)
class WalletTransactionLockTest {

    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    private WalletTransactionServiceImpl service;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new WalletTransactionServiceImpl(walletTransactionRepository, walletRepository,
                userRepository, entityManager);
        wallet = new Wallet();
        wallet.setId(5L);
        wallet.setBalance(new BigDecimal("100.00"));
    }

    private WalletTransactionRequest request(WalletTransactionType type, BigDecimal amount) {
        return WalletTransactionRequest.builder()
                .wallet(wallet).type(type).amount(amount).description("test")
                .build();
    }

    @Test
    void createTransaction_locksWalletRowBeforeMutatingBalance() {
        when(entityManager.find(Wallet.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(wallet);
        when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createTransaction(request(WalletTransactionType.BUY_CARBON_CREDIT, new BigDecimal("40.00")));

        verify(entityManager).find(Wallet.class, 5L, LockModeType.PESSIMISTIC_WRITE);
        // balance mutated on the LOCKED instance
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void createTransaction_debitBeyondBalance_throwsBeforeAnySave() {
        when(entityManager.find(Wallet.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(wallet);

        assertThatThrownBy(() -> service.createTransaction(
                request(WalletTransactionType.BUY_CARBON_CREDIT, new BigDecimal("150.00"))))
                .isInstanceOf(IllegalStateException.class);

        verify(walletTransactionRepository, org.mockito.Mockito.never()).save(any());
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void createTransaction_creditRecordsBeforeAndAfterOnLockedWallet() {
        when(entityManager.find(Wallet.class, 5L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(wallet);
        when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createTransaction(request(WalletTransactionType.ADD_MONEY, new BigDecimal("30.00")));

        ArgumentCaptor<com.carbonx.marketcarbon.model.WalletTransaction> row =
                ArgumentCaptor.forClass(com.carbonx.marketcarbon.model.WalletTransaction.class);
        verify(walletTransactionRepository).save(row.capture());
        assertThat(row.getValue().getBalanceBefore()).isEqualByComparingTo("100.00");
        assertThat(row.getValue().getBalanceAfter()).isEqualByComparingTo("130.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("130.00");
    }
}

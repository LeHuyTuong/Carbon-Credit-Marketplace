package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1.4 — REAL concurrency proof (real MySQL 8, real transactions, real locks, real threads):
 *
 *   Initial: wallet balance = 100
 *   T1: createTransaction(BUY_CARBON_CREDIT, 80)   ┐ both start together
 *   T2: createTransaction(BUY_CARBON_CREDIT, 80)   ┘
 *
 * Expected invariant: exactly ONE succeeds → final balance = 20,
 * exactly one ledger row of 80. NEVER both-success (balance -60 is impossible
 * because the insufficient-balance guard runs on the LOCKED row).
 *
 * Timeline (InnoDB, PESSIMISTIC_WRITE in createTransaction):
 *   T1 BEGIN; SELECT wallet FOR UPDATE (gets row X-lock); balance=100 ≥ 80 → 20; COMMIT.
 *   T2 BEGIN; SELECT wallet FOR UPDATE → BLOCKS until T1 commits;
 *             then reads balance=20 (current, not stale) → 20 < 80 → IllegalStateException → ROLLBACK.
 * Without the lock both SELECTs would read 100 → both debit → lost update → balance -60.
 */
class WalletDebitConcurrencyIT extends MysqlIntegrationTestBase {

    @Autowired private WalletTransactionService walletTransactionService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long walletId;
    private Long userId;

    private void seedWalletWithBalance100() {
        User user = User.builder()
                .email("p14-debit-" + System.nanoTime() + "@test.com")
                .passwordHash("x")
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setCarbonCreditBalance(BigDecimal.ZERO);
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();
    }

    @AfterEach
    void cleanup() {
        if (walletId == null) return;
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.update("DELETE FROM wallet_transaction WHERE wallet_id = ?", walletId);
        jdbc.update("DELETE FROM wallets WHERE id = ?", walletId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
        walletId = null;
    }

    @Test
    void twoConcurrentDebitsOf80_onBalance100_exactlyOneSucceeds() throws Exception {
        seedWalletWithBalance100();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        try {
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    try {
                        // call through the Spring PROXY from this thread → real @Transactional + real lock
                        Wallet lockedRef = walletRepository.findById(walletId).orElseThrow();
                        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                                .wallet(lockedRef)
                                .type(WalletTransactionType.BUY_CARBON_CREDIT)
                                .amount(new BigDecimal("80.00"))
                                .description("P1.4 concurrent debit")
                                .build());
                        successes.incrementAndGet();
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                    return null;
                }));
            }
            startGate.countDown(); // release both threads together
            for (Future<Void> f : futures) f.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // --- assert DATABASE STATE, not mock interactions ---
        Wallet after = walletRepository.findById(walletId).orElseThrow();
        assertThat(successes.get()).as("exactly one debit must succeed").isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
        assertThat(after.getBalance()).as("final balance must be 20, never -60")
                .isEqualByComparingTo(new BigDecimal("20.00"));

        List<WalletTransaction> ledger = walletTransactionRepository.findAll().stream()
                .filter(t -> t.getWallet().getId().equals(walletId))
                .toList();
        assertThat(ledger).as("exactly one 80-debit ledger row").hasSize(1);
        assertThat(ledger.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(ledger.get(0).getBalanceBefore()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(ledger.get(0).getBalanceAfter()).isEqualByComparingTo(new BigDecimal("20.00"));
    }
}

package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.repository.OrderRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1.3 — REAL concurrency proof: two threads complete the SAME order simultaneously.
 *
 * Timeline (completeOrder loads the order WITH a pessimistic lock):
 *   T1 BEGIN; SELECT order FOR UPDATE → PENDING → settles (debits/credits/issues,
 *             marks SUCCESS) → COMMIT.
 *   T2 BEGIN; SELECT order FOR UPDATE → BLOCKS on T1's row lock;
 *             after T1 commits, T2 reads status=SUCCESS → returns as no-op.
 *
 * Expected DB state afterwards (asserted, not mocked):
 *   order.status            = SUCCESS
 *   listing.quantity        = 7        (10 − 3, decremented ONCE)
 *   buyer  balance          = 194.00   (200 − 6, debited ONCE)
 *   seller balance          = 106.00   (100 + 6, credited ONCE)
 *   ledger rows             = exactly 2 (one BUY 6.00, one SELL 6.00)
 *   credits issued to buyer = exactly 3 rows (issuance ran ONCE)
 *
 * Before B3 the idempotency check ran BEFORE the lock: both threads read PENDING
 * and both settled → double money, double issuance.
 */
class OrderSettlementConcurrencyIT extends MysqlIntegrationTestBase {

    @Autowired private OrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CarbonCreditRepository carbonCreditRepository;
    @Autowired private MarketplaceListingRepository marketplaceListingRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long buyerUserId, sellerUserId, buyerCompanyId, sellerCompanyId;
    private Long buyerWalletId, sellerWalletId;
    private Long sourceCreditId, listingId, orderId, projectId;

    private User newUser(String prefix) {
        return userRepository.save(User.builder()
                .email(prefix + "-" + System.nanoTime() + "@test.com")
                .passwordHash("x")
                .status(com.carbonx.marketcarbon.common.USER_STATUS.ACTIVE)
                .build());
    }

    private Wallet newWallet(User user, Company company, String balance, String credits) {
        Wallet w = new Wallet();
        w.setUser(user);
        w.setCompany(company); // completeOrder resolves wallets BY COMPANY
        w.setBalance(new BigDecimal(balance));
        w.setCarbonCreditBalance(new BigDecimal(credits));
        w = walletRepository.save(w);
        return w;
    }

    private void seedTrade() {
        User buyer = newUser("p13buyer");
        User seller = newUser("p13seller");
        buyerUserId = buyer.getId();
        sellerUserId = seller.getId();

        Company buyerCompany = companyRepository.save(Company.builder()
                .user(buyer).companyName("P13 Buyer Co").businessLicense("BL-B").taxCode("TX-B").build());
        Company sellerCompany = companyRepository.save(Company.builder()
                .user(seller).companyName("P13 Seller Co").businessLicense("BL-S").taxCode("TX-S").build());
        buyerCompanyId = buyerCompany.getId();
        sellerCompanyId = sellerCompany.getId();

        Wallet buyerWallet = newWallet(buyer, buyerCompany, "200.00", "0");
        Wallet sellerWallet = newWallet(seller, sellerCompany, "100.00", "10");
        buyerWalletId = buyerWallet.getId();
        sellerWalletId = sellerWallet.getId();

        Project project = projectRepository.save(Project.builder()
                .title("P13 Project")
                .description("P1.3 settlement concurrency")
                .status(ProjectStatus.OPEN)
                .startedDate(LocalDate.now())
                .build());
        projectId = project.getId();

        CarbonCredit sourceCredit = carbonCreditRepository.save(CarbonCredit.builder()
                .creditCode("P13-" + System.nanoTime())
                .company(sellerCompany)
                .project(project)
                .status(CreditStatus.AVAILABLE)
                .amount(new BigDecimal("10"))
                .listedAmount(new BigDecimal("10"))
                .carbonCredit(new BigDecimal("10"))
                .tCo2e(BigDecimal.ONE)
                .vintageYear(2026)
                .name("P13 source credit")
                .currentPrice(2.0)
                .build());
        sourceCreditId = sourceCredit.getId();

        MarketPlaceListing listing = marketplaceListingRepository.save(MarketPlaceListing.builder()
                .company(sellerCompany)
                .carbonCredit(sourceCredit)
                .originalQuantity(new BigDecimal("10"))
                .quantity(new BigDecimal("10"))
                .soldQuantity(BigDecimal.ZERO)
                .pricePerCredit(new BigDecimal("2.00"))
                .status(ListingStatus.AVAILABLE)
                .expiresAt(LocalDate.now().plusDays(30))
                .build());
        listingId = listing.getId();

        Order order = orderRepository.save(Order.builder()
                .company(buyerCompany)
                .marketplaceListing(listing)
                .carbonCredit(sourceCredit)
                .orderType(com.carbonx.marketcarbon.common.OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("2.00"))
                .totalPrice(new BigDecimal("6.00"))
                .platformFee(new BigDecimal("0.30"))
                .sellerPayout(new BigDecimal("6.00"))
                .build());
        orderId = order.getId();
    }

    @AfterEach
    void cleanup() {
        if (orderId == null) return;
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.update("DELETE FROM carbon_credits WHERE source_credit_id = ?", sourceCreditId);
        jdbc.update("DELETE FROM carbon_credits WHERE id = ?", sourceCreditId);
        jdbc.update("DELETE FROM credit_serial_counter WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
        jdbc.update("DELETE FROM marketplace_listings WHERE id = ?", listingId);
        jdbc.update("DELETE FROM wallet_transaction WHERE wallet_id IN (?, ?)", buyerWalletId, sellerWalletId);
        jdbc.update("DELETE FROM wallets WHERE id IN (?, ?)", buyerWalletId, sellerWalletId);
        jdbc.update("DELETE FROM company WHERE id IN (?, ?)", buyerCompanyId, sellerCompanyId);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", buyerUserId, sellerUserId);
        jdbc.update("DELETE FROM project WHERE id = ?", projectId);
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void twoConcurrentCompletions_ofTheSameOrder_settleExactlyOnce() throws Exception {
        seedTrade();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger noOpReturns = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        try {
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    try {
                        orderService.completeOrder(orderId); // through the proxy: real tx + locks
                    } catch (IllegalStateException raceLost) {
                        // acceptable: if both threads race before any commit, the losing
                        // settlement's ledger guard can reject — but exactly one must win
                        errors.incrementAndGet();
                    } catch (Exception ok) {
                        // idempotent no-op path throws nothing; defensive counter
                        errors.incrementAndGet();
                    }
                    return null;
                }));
            }
            startGate.countDown();
            for (Future<Void> f : futures) f.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // ---------- assert DATABASE STATE ----------
        Order order = orderRepository.findById(orderId).orElseThrow();
        MarketPlaceListing listing = marketplaceListingRepository.findById(listingId).orElseThrow();
        Wallet buyerWallet = walletRepository.findById(buyerWalletId).orElseThrow();
        Wallet sellerWallet = walletRepository.findById(sellerWalletId).orElseThrow();

        assertThat(order.getOrderStatus()).as("order must end SUCCESS").isEqualTo(OrderStatus.SUCCESS);
        assertThat(listing.getQuantity()).as("listing decremented exactly once").isEqualByComparingTo(new BigDecimal("7"));
        assertThat(listing.getSoldQuantity()).isEqualByComparingTo(new BigDecimal("3"));

        assertThat(buyerWallet.getBalance()).as("buyer debited exactly once (200-6)")
                .isEqualByComparingTo(new BigDecimal("194.00"));
        assertThat(sellerWallet.getBalance()).as("seller credited exactly once (100+6)")
                .isEqualByComparingTo(new BigDecimal("106.00"));
        assertThat(buyerWallet.getCarbonCreditBalance()).as("buyer received 3 credits once")
                .isEqualByComparingTo(new BigDecimal("3"));

        List<WalletTransaction> ledger = walletTransactionRepository.findAll().stream()
                .filter(t -> t.getWallet().getId().equals(buyerWalletId)
                        || t.getWallet().getId().equals(sellerWalletId))
                .toList();
        assertThat(ledger).as("exactly one BUY + one SELL row").hasSize(2);
        assertThat(ledger).anySatisfy(t -> {
            assertThat(t.getTransactionType()).isEqualTo(WalletTransactionType.BUY_CARBON_CREDIT);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("6.00"));
        });
        assertThat(ledger).anySatisfy(t -> {
            assertThat(t.getTransactionType()).isEqualTo(WalletTransactionType.SELL_CARBON_CREDIT);
            assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("6.00"));
        });

        Integer issued = jdbc.queryForObject(
                "SELECT COUNT(*) FROM carbon_credits WHERE company_id = ? AND source_credit_id = ?",
                Integer.class, buyerCompanyId, sourceCreditId);
        assertThat(issued).as("issuance ran exactly once (3 unit credits)").isEqualTo(3);
    }
}

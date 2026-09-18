package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.MarketplaceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the N+1 fixed in OPTIMIZATION_LOG.md (GET /api/v1/marketplace:
 * 3 802 queries per request → single query via findByStatusAndExpiresAtAfterWithDetails).
 *
 * Nothing in the suite currently fails when a JOIN FETCH is dropped — the endpoint keeps
 * returning identical JSON, only slower. This class turns that silent regression into a
 * red test.
 *
 * Two design decisions that make the guard trustworthy:
 *
 * 1. It asserts the query count is CONSTANT IN N, not equal to some literal. An absolute
 *    number ("must be exactly 1") breaks on a Hibernate upgrade, a dialect change or an
 *    added association, and gets bumped by whoever is annoyed by it — which quietly
 *    destroys the guard. Growth with row count is the actual definition of N+1, so that is
 *    what is measured: read at 2 listings, read at 4, demand the same number.
 *
 * 2. Every listing gets its OWN company, project and carbon credit. Sharing them would hide
 *    the defect: Hibernate's first-level cache resolves the second listing's association
 *    from memory, so lazy loading over 4 listings that share one company issues one extra
 *    query, not four, and a broken query looks fixed.
 *
 * n1IsDetectable() is the negative control: the same read done lazily, in test code, so the
 * guard is proven able to fail. Without it a guard that always passes is indistinguishable
 * from a guard that works.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MarketplaceQueryCountIT extends MysqlIntegrationTestBase {

    @Autowired private MarketplaceService marketplaceService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CarbonCreditRepository carbonCreditRepository;
    @Autowired private MarketplaceListingRepository marketplaceListingRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager em;

    private final List<Long> listingIds = new ArrayList<>();
    private final List<Long> creditIds = new ArrayList<>();
    private final List<Long> companyIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> projectIds = new ArrayList<>();

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    /** Statements issued while {@code read} runs, measured from a cleared counter. */
    private long queriesFor(Runnable read) {
        Statistics stats = statistics();
        stats.clear();
        read.run();
        return stats.getPrepareStatementCount();
    }

    /**
     * One listing with a private company, project and credit — see the class comment on why
     * they must not be shared.
     */
    private void seedListing(String tag) {
        long unique = System.nanoTime();

        User user = userRepository.save(User.builder()
                .email(tag + "-" + unique + "@test.com")
                .passwordHash("x")
                .status(com.carbonx.marketcarbon.common.USER_STATUS.ACTIVE)
                .build());
        userIds.add(user.getId());

        Company company = companyRepository.save(Company.builder()
                .user(user)
                .companyName("QC Co " + tag)
                .businessLicense("BL-" + unique)
                .taxCode("TX-" + unique)
                .build());
        companyIds.add(company.getId());

        Project project = projectRepository.save(Project.builder()
                .title("QC Project " + tag)
                .description("query-count guard")
                .status(ProjectStatus.OPEN)
                .startedDate(LocalDate.now())
                .build());
        projectIds.add(project.getId());

        CarbonCredit credit = carbonCreditRepository.save(CarbonCredit.builder()
                .creditCode("QC-" + unique)
                .company(company)
                .project(project)
                .status(CreditStatus.AVAILABLE)
                .amount(new BigDecimal("10"))
                .listedAmount(new BigDecimal("10"))
                .carbonCredit(new BigDecimal("10"))
                .tCo2e(BigDecimal.ONE)
                .vintageYear(2026)
                .name("QC credit " + tag)
                .currentPrice(new BigDecimal("2.00"))
                .build());
        creditIds.add(credit.getId());

        MarketPlaceListing listing = marketplaceListingRepository.save(MarketPlaceListing.builder()
                .company(company)
                .carbonCredit(credit)
                .originalQuantity(new BigDecimal("10"))
                .quantity(new BigDecimal("10"))
                .soldQuantity(BigDecimal.ZERO)
                .pricePerCredit(new BigDecimal("2.00"))
                .status(ListingStatus.AVAILABLE)
                .expiresAt(LocalDate.now().plusDays(30))
                .build());
        listingIds.add(listing.getId());
    }

    private void seedListings(int count, String tag) {
        for (int i = 0; i < count; i++) seedListing(tag + i);
    }

    /** The lazy read the production query replaced — reproduces the defect on purpose. */
    private void lazyRead() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<MarketPlaceListing> listings = em.createQuery(
                            "SELECT ml FROM MarketPlaceListing ml "
                                    + "WHERE ml.status = :status AND ml.expiresAt > :now",
                            MarketPlaceListing.class)
                    .setParameter("status", ListingStatus.AVAILABLE)
                    .setParameter("now", LocalDate.now())
                    .getResultList();
            for (MarketPlaceListing listing : listings) {
                listing.getCompany().getCompanyName();
                CarbonCredit credit = listing.getCarbonCredit();
                if (credit.getProject() != null) credit.getProject().getTitle();
            }
        });
    }

    @AfterEach
    void cleanup() {
        if (listingIds.isEmpty()) return;
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        deleteAll("marketplace_listings", listingIds);
        deleteAll("carbon_credits", creditIds);
        deleteAll("company", companyIds);
        deleteAll("users", userIds);
        deleteAll("project", projectIds);
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
        listingIds.clear();
        creditIds.clear();
        companyIds.clear();
        userIds.clear();
        projectIds.clear();
    }

    private void deleteAll(String table, List<Long> ids) {
        for (Long id : ids) jdbc.update("DELETE FROM " + table + " WHERE id = ?", id);
    }

    @Test
    void getActiveListing_doesNotIssueMoreQueriesAsListingsAreAdded() {
        seedListings(2, "a");
        marketplaceService.getActiveListing(); // warm-up: excludes one-off statement preparation

        long atTwo = queriesFor(marketplaceService::getActiveListing);

        seedListings(2, "b");
        long atFour = queriesFor(marketplaceService::getActiveListing);

        assertThat(atFour)
                .as("getActiveListing must load every association up front; %d queries at 2 "
                        + "listings but %d at 4 means a JOIN FETCH was lost", atTwo, atFour)
                .isEqualTo(atTwo);
    }

    @Test
    void n1IsDetectable() {
        seedListings(2, "c");
        lazyRead(); // warm-up

        long atTwo = queriesFor(this::lazyRead);

        seedListings(2, "d");
        long atFour = queriesFor(this::lazyRead);

        assertThat(atFour)
                .as("negative control: lazy loading MUST grow with row count, otherwise the "
                        + "guard above cannot fail and proves nothing")
                .isGreaterThan(atTwo);
    }
}

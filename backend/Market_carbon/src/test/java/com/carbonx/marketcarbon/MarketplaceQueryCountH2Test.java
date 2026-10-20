package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Query-count guard for the marketplace listing read, on an in-memory H2 JPA slice.
 *
 * OPTIMIZATION_LOG.md records GET /api/v1/marketplace dropping from 3 802 queries per
 * request to one, by replacing {@code findByStatusAndExpiresAtAfter} with the JOIN FETCH
 * variant. Nothing in the suite fails if that JOIN FETCH is dropped again: the endpoint
 * keeps returning byte-identical JSON, only slower. This class makes that regression red.
 *
 * <h2>Why H2 here when the project banned H2 elsewhere</h2>
 * MysqlIntegrationTestBase rejects H2 because it cannot reproduce InnoDB row locks,
 * REPEATABLE READ or DECIMAL semantics — all true, and all irrelevant to this assertion.
 * What is measured is how many statements HIBERNATE decides to issue, which is a property
 * of the mapping and the query, not of the database. The database only has to answer.
 * Anything about locking or money arithmetic still belongs on Testcontainers MySQL.
 *
 * <h2>Two decisions that make the guard trustworthy</h2>
 * <ol>
 *   <li><b>It asserts the count is CONSTANT IN N, never equal to a literal.</b> "Must be
 *       exactly 1" breaks on a Hibernate upgrade or an added association, and then gets
 *       bumped by whoever is annoyed by it — which silently retires the guard. Growth with
 *       row count IS the definition of N+1, so that is what is asserted: read at 2 listings,
 *       read at 4, demand the same number.</li>
 *   <li><b>Every listing gets its own company, project and credit.</b> Sharing them hides
 *       the defect: Hibernate's first-level cache serves the second listing's association
 *       from memory, so four listings sharing one company cost one extra query rather than
 *       four, and a broken query looks fixed.</li>
 * </ol>
 *
 * {@link #theGuardCanFail()} is the negative control, and it is the only caller of the
 * non-fetching {@code findByStatusAndExpiresAtAfter}. A guard that has never been observed
 * to fail is indistinguishable from a guard that cannot.
 *
 * <h2>Measured</h2>
 * <pre>
 *   findByStatusAndExpiresAtAfterWithDetails   2 listings → 1    4 listings → 1
 *   findByStatusAndExpiresAtAfter              2 listings → 7    4 listings → 13
 * </pre>
 * The lazy figures are 1 + 3N: the root select, then company, credit and project once per
 * listing. Production came out lower per row — OPTIMIZATION_LOG.md measured 3 802 queries
 * over 2 000 listings, under 2 per listing — because that seed shared 10 companies across
 * all 2 000 rows, so the first-level cache answered most company lookups. That is the same
 * effect as decision 2 above, seen from the other side: sharing owners understates N+1,
 * which is why this test refuses to share them.
 */
// showSql must be switched off HERE, not through spring.jpa.show-sql below: @DataJpaTest
// applies its own attribute afterwards and wins. Set it back to true to see WHICH query
// repeats rather than how many there are.
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:querycount;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        // generate_statistics also makes Hibernate log a Session Metrics block at INFO every
        // time a session closes — dozens of all-zero blocks during context startup alone.
        // The counters this test reads are unaffected; only the logging is silenced.
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
})
class MarketplaceQueryCountH2Test {

    @Autowired private MarketplaceListingRepository listingRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @PersistenceContext private EntityManager em;

    /**
     * Statements Hibernate issues while {@code read} runs.
     *
     * <p>{@code flush() + clear()} is what makes the measurement honest: the whole test
     * method shares one transaction, so without clearing, every entity seeded above is still
     * managed and a lazy association resolves from memory without touching the database.
     * The defect would be invisible.
     */
    private long queriesFor(Supplier<List<MarketPlaceListing>> read) {
        em.flush();
        em.clear();
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        read.get().forEach(MarketplaceQueryCountH2Test::renderListing);
        return stats.getPrepareStatementCount();
    }

    /** The associations MarketplaceServiceImpl#buildListingResponse walks for each row. */
    private static void renderListing(MarketPlaceListing listing) {
        listing.getCompany().getCompanyName();
        CarbonCredit credit = listing.getCarbonCredit();
        if (credit != null && credit.getProject() != null) credit.getProject().getTitle();
    }

    private List<MarketPlaceListing> withDetails() {
        return listingRepository.findByStatusAndExpiresAtAfterWithDetails(
                ListingStatus.AVAILABLE, LocalDate.now());
    }

    private List<MarketPlaceListing> withoutDetails() {
        return listingRepository.findByStatusAndExpiresAtAfter(
                ListingStatus.AVAILABLE, LocalDate.now());
    }

    /** One listing owning a private company, project and credit — see the class comment. */
    private void seedListing(String tag) {
        User user = em.merge(User.builder()
                .email(tag + "@test.com")
                .passwordHash("x")
                .status(com.carbonx.marketcarbon.common.USER_STATUS.ACTIVE)
                .build());

        Company company = em.merge(Company.builder()
                .user(user)
                .companyName("Co " + tag)
                .businessLicense("BL-" + tag)
                .taxCode("TX-" + tag)
                .build());

        Project project = em.merge(Project.builder()
                .title("Project " + tag)
                .description("query-count guard")
                .status(ProjectStatus.OPEN)
                .startedDate(LocalDate.now())
                .build());

        CarbonCredit credit = em.merge(CarbonCredit.builder()
                .creditCode("QC-" + tag)
                .company(company)
                .project(project)
                .status(CreditStatus.AVAILABLE)
                .amount(new BigDecimal("10"))
                .listedAmount(new BigDecimal("10"))
                .carbonCredit(new BigDecimal("10"))
                .tCo2e(BigDecimal.ONE)
                .vintageYear(2026)
                .name("Credit " + tag)
                .currentPrice(new BigDecimal("2.00"))
                .build());

        em.merge(MarketPlaceListing.builder()
                .company(company)
                .carbonCredit(credit)
                .originalQuantity(new BigDecimal("10"))
                .quantity(new BigDecimal("10"))
                .soldQuantity(BigDecimal.ZERO)
                .pricePerCredit(new BigDecimal("2.00"))
                .status(ListingStatus.AVAILABLE)
                .expiresAt(LocalDate.now().plusDays(30))
                .build());
    }

    private void seedListings(int count, String tag) {
        for (int i = 0; i < count; i++) seedListing(tag + i);
    }

    @Test
    void activeListingRead_doesNotIssueMoreQueriesAsListingsAreAdded() {
        seedListings(2, "a");
        long atTwo = queriesFor(this::withDetails);

        seedListings(2, "b");
        long atFour = queriesFor(this::withDetails);

        assertThat(atFour)
                .as("the marketplace read must load every association up front; %d queries "
                        + "at 2 listings but %d at 4 means a JOIN FETCH was lost", atTwo, atFour)
                .isEqualTo(atTwo);
    }

    @Test
    void theGuardCanFail() {
        seedListings(2, "c");
        long atTwo = queriesFor(this::withoutDetails);

        seedListings(2, "d");
        long atFour = queriesFor(this::withoutDetails);

        assertThat(atFour)
                .as("negative control: without JOIN FETCH the count MUST grow with row count "
                        + "(%d at 2 listings, %d at 4), otherwise the guard above cannot fail "
                        + "and proves nothing", atTwo, atFour)
                .isGreaterThan(atTwo);
    }
}

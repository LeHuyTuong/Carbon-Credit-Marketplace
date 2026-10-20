package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import com.carbonx.marketcarbon.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-2 lease guard: the CAS queries themselves, not mocks.
 *
 * <p>The bug this prevents is a missing {@code updatedAt} refresh in {@code markRetrying}:
 * the row stays "stale" immediately after a claim, so a second {@code /retry} can claim the
 * same distribution again. These tests run the real JPQL against an H2 schema and assert
 * the lease/token transitions. InnoDB locking semantics remain covered by the MySQL
 * Testcontainers tests; here only the SQL effects matter.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProfitDistributionClaimH2Test {

    @Autowired private EntityManager entityManager;
    @Autowired private ProfitDistributionRepository repository;
    @Autowired private ProfitDistributionDetailRepository detailRepository;

    @Test
    void markRetryingRefreshesHeartbeatSoTheJustClaimedRowIsNotStale() {
        ProfitDistribution distribution = persistFailedDistribution();
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        int claimed = repository.markRetrying(
                distribution.getId(),
                ProfitDistributionStatus.PROCESSING,
                ProfitDistributionStatus.FAILED,
                "claim-1",
                now);
        assertThat(claimed).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        ProfitDistribution fresh = repository.findById(distribution.getId()).orElseThrow();
        assertThat(fresh.getStatus()).isEqualTo(ProfitDistributionStatus.PROCESSING);
        assertThat(fresh.getClaimToken()).isEqualTo("claim-1");
        assertThat(fresh.getUpdatedAt()).isAfterOrEqualTo(now.minusSeconds(1));

        // The row was stale before the claim (created just now, but the claim timestamp is
        // the lease). A second retry using staleBefore=now-15m must NOT win.
        int secondClaim = repository.claimStaleProcessing(
                distribution.getId(),
                ProfitDistributionStatus.PROCESSING,
                now.minusMinutes(15),
                now.plusSeconds(1),
                "claim-2");
        assertThat(secondClaim)
                .as("a freshly claimed distribution is not stale, so a second retry must lose")
                .isZero();
    }

    @Test
    void staleClaimReplacesTheTokenAndTheOldWorkerCannotHeartbeatOrFinish() {
        ProfitDistribution distribution = persistFailedDistribution();
        OffsetDateTime claimTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS).minusMinutes(30);

        assertThat(repository.markRetrying(
                distribution.getId(),
                ProfitDistributionStatus.PROCESSING,
                ProfitDistributionStatus.FAILED,
                "claim-old",
                claimTime)).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        int reclaimed = repository.claimStaleProcessing(
                distribution.getId(),
                ProfitDistributionStatus.PROCESSING,
                now.minusMinutes(15),
                now,
                "claim-new");
        assertThat(reclaimed).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.touchProcessing(
                distribution.getId(),
                ProfitDistributionStatus.PROCESSING,
                now.plusSeconds(1),
                "claim-old"))
                .as("the old worker must not keep the lease alive")
                .isZero();

        assertThat(repository.finishIfClaimed(
                distribution.getId(),
                ProfitDistributionStatus.COMPLETED,
                ProfitDistributionStatus.PROCESSING,
                "claim-old",
                now.plusSeconds(1)))
                .as("the old worker must not complete the new claim")
                .isZero();

        assertThat(repository.finishIfClaimed(
                distribution.getId(),
                ProfitDistributionStatus.COMPLETED,
                ProfitDistributionStatus.PROCESSING,
                "claim-new",
                now.plusSeconds(1)))
                .isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        ProfitDistribution finished = repository.findById(distribution.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(ProfitDistributionStatus.COMPLETED);
        assertThat(finished.getClaimToken()).isNull();
    }

    @Test
    void legacyCompletedDistributionIsRetryableOnlyWhenAnOwnerIsStillUnsettled() {
        ProfitDistribution distribution = persistFailedDistribution();
        User ownerUser = new User();
        ownerUser.setEmail("owner-" + System.nanoTime() + "@example.com");
        ownerUser.setPasswordHash("hash");
        ownerUser.setStatus(USER_STATUS.ACTIVE);
        entityManager.persist(ownerUser);

        EVOwner owner = new EVOwner();
        owner.setName("Owner");
        owner.setUser(ownerUser);
        owner.setPhone("0900000000");
        owner.setCountry("VN");
        owner.setAddress("Test address");
        owner.setBirthDate(LocalDate.of(1990, 1, 1));
        owner.setEmail(ownerUser.getEmail());
        owner.setDocumentType(IDType.CCCD);
        owner.setDocumentNumber("012345678901");
        owner.setGender(Gender.MALE);
        entityManager.persist(owner);

        ProfitDistributionDetail failed = detail(distribution, owner, "FAILED");
        entityManager.persist(failed);
        entityManager.flush();

        assertThat(detailRepository.existsUnsettledByDistributionId(distribution.getId()))
                .as("a legacy COMPLETED row with a FAILED owner must stay retryable")
                .isTrue();

        failed.setStatus("SKIPPED");
        entityManager.flush();

        assertThat(detailRepository.existsUnsettledByDistributionId(distribution.getId()))
                .as("settled details (SUCCESS/SKIPPED) must not keep the distribution retryable")
                .isFalse();
    }

    private ProfitDistributionDetail detail(ProfitDistribution distribution, EVOwner owner, String status) {
        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setDistribution(distribution);
        detail.setEvOwner(owner);
        detail.setMoneyAmount(BigDecimal.ONE);
        detail.setCreditAmount(BigDecimal.ONE);
        detail.setEnergyAmount(BigDecimal.ONE);
        detail.setStatus(status);
        return detail;
    }

    private ProfitDistribution persistFailedDistribution() {
        User companyUser = new User();
        companyUser.setEmail("company-" + System.nanoTime() + "@example.com");
        companyUser.setPasswordHash("hash");
        companyUser.setStatus(USER_STATUS.ACTIVE);
        entityManager.persist(companyUser);

        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setCompanyUser(companyUser);
        distribution.setTotalMoneyDistributed(BigDecimal.ZERO);
        distribution.setTotalCreditsDistributed(BigDecimal.ZERO);
        distribution.setStatus(ProfitDistributionStatus.FAILED);
        entityManager.persist(distribution);
        entityManager.flush();
        return distribution;
    }
}

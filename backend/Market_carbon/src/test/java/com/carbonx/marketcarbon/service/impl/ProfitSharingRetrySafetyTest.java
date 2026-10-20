package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.repository.ProfitDistributionDetailRepository;
import com.carbonx.marketcarbon.repository.ProfitDistributionRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.DynamicPricingService;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.ProfitSharingTaskDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F2/F3/F4/F6/F8 guards for the retry path, plus the round-2 lease guards:
 * <ul>
 *   <li>an old distribution cannot be retried after a newer one exists;</li>
 *   <li>a PROCESSING distribution is only retried after its heartbeat is stale;</li>
 *   <li>the claim refresh itself makes a second retry see a fresh heartbeat;</li>
 *   <li>PENDING details count as unpaid and empty details never mark the report paid;</li>
 *   <li>ownership is validated on the request thread before work is dispatched;</li>
 *   <li>a legacy distribution without a report link can still pay its owners;</li>
 *   <li>a legacy COMPLETED distribution with FAILED owners can be retried.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProfitSharingRetrySafetyTest {

    @Mock private EmissionReportRepository emissionReportRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EVOwnerRepository evOwnerRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletServiceImpl walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private ProfitDistributionRepository profitDistributionRepository;
    @Mock private ProfitDistributionDetailRepository profitDistributionDetailRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private EmailService emailService;
    @Mock private DynamicPricingService dynamicPricingService;

    private final List<Runnable> dispatched = new ArrayList<>();
    private ProfitSharingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new ProfitSharingServiceImpl(
                emissionReportRepository,
                vehicleRepository,
                evOwnerRepository,
                userRepository,
                walletService,
                walletRepository,
                profitDistributionRepository,
                profitDistributionDetailRepository,
                projectRepository,
                companyRepository,
                properties(),
                emailService,
                dynamicPricingService,
                transactionManager,
                new ProfitSharingTaskDispatcher(dispatched::add)));
        ReflectionTestUtils.setField(service, "requiresNewTransaction", noOpTemplate());
    }

    @Test
    void oldDistributionIsRejectedBeforeAnyWorkIsDispatched() {
        User companyUser = companyUser(1L);
        ProfitDistribution old = distribution(7L, companyUser, report(99L), ProfitDistributionStatus.FAILED);
        ProfitDistribution newer = distribution(8L, companyUser, report(99L), ProfitDistributionStatus.FAILED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(old));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(newer));

        assertThatThrownBy(() -> service.requestRetry(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("superseded by distribution #8");

        assertThat(dispatched).isEmpty();
        verify(profitDistributionRepository, never()).markRetrying(any(), any(), any(), any(), any());
    }

    @Test
    void freshProcessingDistributionIsRejectedUntilItsHeartbeatIsStale() {
        User companyUser = companyUser(1L);
        ProfitDistribution processing = distribution(7L, companyUser, report(99L),
                ProfitDistributionStatus.PROCESSING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(processing));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(processing));
        when(profitDistributionRepository.claimStaleProcessing(eq(7L), any(), any(), any(), anyString()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.requestRetry(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still running");

        assertThat(dispatched).isEmpty();
    }

    @Test
    void staleProcessingDistributionIsClaimedAndDispatched() {
        User companyUser = companyUser(1L);
        ProfitDistribution processing = distribution(7L, companyUser, report(99L),
                ProfitDistributionStatus.PROCESSING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(processing));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(processing));
        when(profitDistributionRepository.claimStaleProcessing(eq(7L), any(), any(), any(), anyString()))
                .thenReturn(1);

        service.requestRetry(1L, 7L);

        assertThat(dispatched).hasSize(1);
        verify(profitDistributionRepository).claimStaleProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING),
                any(OffsetDateTime.class), any(OffsetDateTime.class), anyString());
    }

    @Test
    void claimRefreshesHeartbeatSoASecondRetryCannotReclaimTheSameRun() {
        User companyUser = companyUser(1L);
        ProfitDistribution distribution = distribution(7L, companyUser, report(99L),
                ProfitDistributionStatus.FAILED);

        // Fake repository state: FAILED with a 30-minute-old heartbeat.
        AtomicReference<ProfitDistributionStatus> status =
                new AtomicReference<>(ProfitDistributionStatus.FAILED);
        AtomicReference<OffsetDateTime> updatedAt =
                new AtomicReference<>(OffsetDateTime.now().minusMinutes(30));

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenAnswer(inv -> {
            distribution.setStatus(status.get());
            distribution.setUpdatedAt(updatedAt.get());
            return Optional.of(distribution);
        });
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenAnswer(inv -> Optional.of(distribution));
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.FAILED),
                anyString(), any(OffsetDateTime.class)))
                .thenAnswer(inv -> {
                    if (status.get() != ProfitDistributionStatus.FAILED) return 0;
                    status.set(ProfitDistributionStatus.PROCESSING);
                    updatedAt.set(inv.getArgument(4));
                    return 1;
                });
        when(profitDistributionRepository.claimStaleProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING),
                any(OffsetDateTime.class), any(OffsetDateTime.class), anyString()))
                .thenAnswer(inv -> {
                    OffsetDateTime staleBefore = inv.getArgument(2);
                    if (status.get() == ProfitDistributionStatus.PROCESSING
                            && updatedAt.get().isBefore(staleBefore)) {
                        updatedAt.set(inv.getArgument(3));
                        return 1;
                    }
                    return 0;
                });

        service.requestRetry(1L, 7L);
        assertThat(dispatched).hasSize(1);

        // The claim wrote updatedAt=now. The second request must see PROCESSING with a
        // fresh heartbeat and be rejected instead of claiming/dispatching a second run.
        assertThatThrownBy(() -> service.requestRetry(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still running");
        assertThat(dispatched).hasSize(1);
    }

    @Test
    void pendingDetailIsTreatedAsUnpaidAndPaidOnRetry() {
        User companyUser = companyUser(1L);
        Company company = company(10L);
        Wallet wallet = wallet();
        EmissionReport report = report(99L);
        ProfitDistribution distribution = distribution(7L, companyUser, report,
                ProfitDistributionStatus.FAILED);
        ProfitDistributionDetail pending = detail(55L, distribution, "PENDING");

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(distribution));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(distribution));
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.FAILED),
                anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), any(), eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L))
                .thenReturn(List.of(pending));
        when(walletService.findWalletByUser(companyUser)).thenReturn(wallet);
        when(walletRepository.findById(3L)).thenReturn(Optional.of(wallet));
        when(emissionReportRepository.findById(99L)).thenReturn(Optional.of(report));
        when(emissionReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        doAnswer(inv -> {
            pending.setStatus("SUCCESS");
            return true;
        }).when(service).processPayoutForOwner(
                eq(distribution), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(wallet), eq(99L), any(), eq(company), eq(55L));

        service.requestRetry(1L, 7L);
        dispatched.get(0).run();

        verify(service).processPayoutForOwner(
                eq(distribution), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(wallet), eq(99L), any(), eq(company), eq(55L));
        assertThat(report.getStatus()).isEqualTo(EmissionStatus.PAID_OUT);
        assertThat(distribution.getStatus()).isEqualTo(ProfitDistributionStatus.COMPLETED);
    }

    @Test
    void emptyDetailsAreNeverMarkedPaidOut() {
        User companyUser = companyUser(1L);
        Company company = company(10L);
        EmissionReport report = report(99L);
        ProfitDistribution distribution = distribution(7L, companyUser, report,
                ProfitDistributionStatus.FAILED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(distribution));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(distribution));
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.FAILED),
                anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), eq(ProfitDistributionStatus.FAILED),
                eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L)).thenReturn(List.of());

        service.requestRetry(1L, 7L);
        dispatched.get(0).run();

        verify(emissionReportRepository, never()).save(any());
        verify(profitDistributionRepository).finishIfClaimed(
                eq(7L), eq(ProfitDistributionStatus.FAILED),
                eq(ProfitDistributionStatus.PROCESSING), anyString(), any());
    }

    @Test
    void anotherCompanysDistributionIsRejectedBeforeDispatch() {
        User caller = companyUser(1L);
        User otherCompanyUser = companyUser(2L);
        ProfitDistribution distribution = distribution(7L, otherCompanyUser, report(99L),
                ProfitDistributionStatus.FAILED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(caller));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(distribution));

        assertThatThrownBy(() -> service.requestRetry(1L, 7L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode().name())
                        .isEqualTo("ACCESS_DENIED"));

        assertThat(dispatched).isEmpty();
    }

    @Test
    void legacyDistributionWithoutReportCanStillPayItsOwners() {
        User companyUser = companyUser(1L);
        Company company = company(10L);
        Wallet wallet = wallet();
        ProfitDistribution legacy = distribution(7L, companyUser, null,
                ProfitDistributionStatus.FAILED);
        legacy.setProject(project(20L));
        ProfitDistributionDetail pending = detail(55L, legacy, "PENDING");

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(legacy));
        when(profitDistributionRepository.findFirstByCompanyUserIdAndProjectIdOrderByIdDesc(1L, 20L))
                .thenReturn(Optional.of(legacy));
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.FAILED),
                anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), any(), eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L))
                .thenReturn(List.of(pending));
        when(walletService.findWalletByUser(companyUser)).thenReturn(wallet);
        when(walletRepository.findById(3L)).thenReturn(Optional.of(wallet));

        doAnswer(inv -> {
            pending.setStatus("SUCCESS");
            return true;
        }).when(service).processPayoutForOwner(
                eq(legacy), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(wallet), eq(null), any(), eq(company), eq(55L));

        service.requestRetry(1L, 7L);
        dispatched.get(0).run();

        assertThat(legacy.getStatus()).isEqualTo(ProfitDistributionStatus.COMPLETED);
        verify(emissionReportRepository, never()).save(any());
    }

    @Test
    void legacyCompletedDistributionWithFailedOwnerIsRetried() {
        User companyUser = companyUser(1L);
        Company company = company(10L);
        Wallet wallet = wallet();
        EmissionReport report = report(99L);
        report.setStatus(EmissionStatus.PAID_OUT);
        ProfitDistribution completed = distribution(7L, companyUser, report,
                ProfitDistributionStatus.COMPLETED);
        ProfitDistributionDetail failed = detail(55L, completed, "FAILED");

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(completed));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(completed));
        when(profitDistributionDetailRepository.existsUnsettledByDistributionId(7L)).thenReturn(true);
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.COMPLETED),
                anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), any(), eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L))
                .thenReturn(List.of(failed));
        when(walletService.findWalletByUser(companyUser)).thenReturn(wallet);
        when(walletRepository.findById(3L)).thenReturn(Optional.of(wallet));
        when(emissionReportRepository.findById(99L)).thenReturn(Optional.of(report));

        doAnswer(inv -> {
            failed.setStatus("SUCCESS");
            return true;
        }).when(service).processPayoutForOwner(
                eq(completed), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(wallet), eq(99L), any(), eq(company), eq(55L));

        service.requestRetry(1L, 7L);
        dispatched.get(0).run();

        verify(service).processPayoutForOwner(
                eq(completed), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(wallet), eq(99L), any(), eq(company), eq(55L));
        assertThat(completed.getStatus()).isEqualTo(ProfitDistributionStatus.COMPLETED);
    }

    @Test
    void completedDistributionWithoutUnsettledDetailsIsStillRejected() {
        User companyUser = companyUser(1L);
        ProfitDistribution completed = distribution(7L, companyUser, report(99L),
                ProfitDistributionStatus.COMPLETED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company(10L)));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(completed));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(completed));
        when(profitDistributionDetailRepository.existsUnsettledByDistributionId(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.requestRetry(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already completed");

        assertThat(dispatched).isEmpty();
        verify(profitDistributionRepository, never()).markRetrying(any(), any(), any(), any(), any());
    }

    private ProfitSharingProperties properties() {
        ProfitSharingProperties properties = new ProfitSharingProperties();
        ProfitSharingProperties.Policy policy = new ProfitSharingProperties.Policy();
        policy.setPricingMode(ProfitSharingProperties.PricingMode.CREDIT);
        policy.setOwnerSharePct(new BigDecimal("0.5"));
        properties.setDefaultPolicy(policy);
        return properties;
    }

    private User companyUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("company" + id + "@example.com");
        return user;
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        company.setCompanyName("Acme");
        return company;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setTitle("Project " + id);
        return project;
    }

    private EmissionReport report(Long id) {
        EmissionReport report = new EmissionReport();
        report.setId(id);
        report.setStatus(EmissionStatus.CREDIT_ISSUED);
        report.setPeriod("2025-10");
        return report;
    }

    private ProfitDistribution distribution(Long id, User companyUser, EmissionReport report,
                                            ProfitDistributionStatus status) {
        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setId(id);
        distribution.setCompanyUser(companyUser);
        distribution.setEmissionReport(report);
        distribution.setStatus(status);
        distribution.setTotalMoneyDistributed(BigDecimal.ZERO);
        distribution.setTotalCreditsDistributed(BigDecimal.ZERO);
        return distribution;
    }

    private ProfitDistributionDetail detail(Long id, ProfitDistribution distribution, String status) {
        EVOwner owner = new EVOwner();
        owner.setId(42L);
        owner.setName("Owner 42");
        User user = new User();
        user.setId(42L);
        user.setEmail("owner@example.com");
        owner.setUser(user);

        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setId(id);
        detail.setDistribution(distribution);
        detail.setEvOwner(owner);
        detail.setStatus(status);
        detail.setMoneyAmount(new BigDecimal("3.00"));
        detail.setCreditAmount(new BigDecimal("600.000000"));
        detail.setEnergyAmount(new BigDecimal("1000.000000"));
        return detail;
    }

    private Wallet wallet() {
        Wallet wallet = new Wallet();
        wallet.setId(3L);
        wallet.setBalance(new BigDecimal("1000.00"));
        return wallet;
    }

    private TransactionTemplate noOpTemplate() {
        TransactionTemplate template = new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                // no resources in this unit test
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                // success
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                // no-op
            }
        });
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}

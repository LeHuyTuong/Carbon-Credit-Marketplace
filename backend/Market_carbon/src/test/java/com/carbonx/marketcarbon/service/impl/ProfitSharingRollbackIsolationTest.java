package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
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
import com.carbonx.marketcarbon.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * RV-2-01: a rollback-only REQUIRES_NEW transaction commits with
 * {@link UnexpectedRollbackException} after {@code doProcessPayoutForOwner} already swallowed the
 * original exception. That commit failure must not escape {@code processPayoutForOwner}, otherwise
 * it aborts the whole payout loop and leaves already-paid owners unaccounted for.
 *
 * <p>Scope: this test covers only the loop-stop fix. Cross-distribution retry/compensation is A7
 * and is intentionally not provided by this method.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProfitSharingRollbackIsolationTest {

    @Mock private EmissionReportRepository emissionReportRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private EVOwnerRepository evOwnerRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private ProfitDistributionRepository profitDistributionRepository;
    @Mock private ProfitDistributionDetailRepository profitDistributionDetailRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ProfitSharingProperties profitSharingProperties;
    @Mock private EmailService emailService;
    @Mock private DynamicPricingService dynamicPricingService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionTemplate requiresNewTransaction;
    @Mock private TransactionStatus transactionStatus;

    private ProfitSharingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfitSharingServiceImpl(
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
                profitSharingProperties,
                emailService,
                dynamicPricingService,
                transactionManager);
        ReflectionTestUtils.setField(service, "requiresNewTransaction", requiresNewTransaction);
    }

    @Test
    void rollbackOnlyCommitIsRecordedAsFailedDetailAndDoesNotEscape() {
        stubFirstCallToThrowThenExecute(new UnexpectedRollbackException("rollback-only"));

        ProfitDistribution distribution = distribution(7L);
        ProfitSharingServiceImpl.OwnerPayoutData payout = payout(42L);

        assertThatCode(() -> service.processPayoutForOwner(distribution, payout, null, 99L, null, null))
                .as("a rollback-only commit must not abort the per-owner loop")
                .doesNotThrowAnyException();

        ArgumentCaptor<ProfitDistributionDetail> captor =
                ArgumentCaptor.forClass(ProfitDistributionDetail.class);
        verify(profitDistributionDetailRepository, times(1)).save(captor.capture());

        ProfitDistributionDetail saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getDistribution()).isSameAs(distribution);
        assertThat(saved.getMoneyAmount()).isEqualByComparingTo("12.50");
        assertThat(saved.getErrorMessage())
                .as("the failure detail must keep the rollback reason for the admin report")
                .contains("rollback-only");
    }

    @Test
    void unexpectedRuntimeExceptionStillPropagatesSoTheDistributionFails() {
        stubFirstCallToThrowThenExecute(new IllegalStateException("database is gone"));

        ProfitDistribution distribution = distribution(7L);
        ProfitSharingServiceImpl.OwnerPayoutData payout = payout(42L);

        assertThatThrownBy(() -> service.processPayoutForOwner(distribution, payout, null, 99L, null, null))
                .as("only rollback-only commits are isolated; programming/DB errors must fail the run")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database is gone");

        verifyNoInteractions(profitDistributionDetailRepository);
    }

    private void stubFirstCallToThrowThenExecute(RuntimeException firstFailure) {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.getAndIncrement() == 0) {
                throw firstFailure;
            }
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(requiresNewTransaction).executeWithoutResult(any());
    }

    private ProfitDistribution distribution(Long id) {
        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setId(id);
        return distribution;
    }

    private ProfitSharingServiceImpl.OwnerPayoutData payout(Long ownerId) {
        return new ProfitSharingServiceImpl.OwnerPayoutData(
                ownerId,
                new BigDecimal("100.000000"),
                new BigDecimal("50.000000"),
                new BigDecimal("12.50"));
    }
}

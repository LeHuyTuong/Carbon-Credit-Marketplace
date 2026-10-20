package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R3-1/R3-3: the rollback-only commit path must execute the real {@code doProcessPayoutForOwner}
 * callback and record exactly one FAILED detail, reusing the existing detail on retry.
 *
 * <p>The test drives a real {@link TransactionTemplate} whose first commit throws
 * {@link UnexpectedRollbackException}, which reproduces the production chain without mocking the
 * template itself: callback runs -> wallet creation fails -> commit fails -> failure detail is
 * written in a second REQUIRES_NEW transaction.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProfitSharingRollbackIsolationTest {

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
    @Mock private ProfitSharingProperties profitSharingProperties;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private EmailService emailService;
    @Mock private DynamicPricingService dynamicPricingService;

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
                transactionManager,
                new ProfitSharingTaskDispatcher(Runnable::run));
    }

    @Test
    void rollbackOnlyCommitIsRecordedAsOneFailedDetailAndDoesNotEscape() {
        useTemplateThatFailsFirstCommit("rollback-only");
        stubWalletCreationFailure();
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfitDistribution distribution = distribution(7L);
        ProfitSharingServiceImpl.OwnerPayoutData payout = payout(42L);

        boolean success = service.processPayoutForOwner(
                distribution, payout, companyWallet(), 99L, null, null, null);

        assertThat(success).as("a rolled-back owner must be reported as failed").isFalse();

        ArgumentCaptor<ProfitDistributionDetail> captor =
                ArgumentCaptor.forClass(ProfitDistributionDetail.class);
        verify(profitDistributionDetailRepository, times(1)).save(captor.capture());

        ProfitDistributionDetail saved = captor.getValue();
        assertThat(saved.getId()).as("first run creates a detail").isNull();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getDistribution()).isSameAs(distribution);
        assertThat(saved.getMoneyAmount()).isEqualByComparingTo("12.50");
        assertThat(saved.getErrorMessage())
                .as("the failure detail must keep the rollback reason for the admin report")
                .contains("rollback-only");
    }

    @Test
    void retryUpdatesTheExistingFailedDetailInsteadOfInsertingASecondRow() {
        useTemplateThatFailsFirstCommit("rollback-only");
        stubWalletCreationFailure();

        ProfitDistribution distribution = distribution(7L);
        ProfitSharingServiceImpl.OwnerPayoutData payout = payout(42L);
        ProfitDistributionDetail existing = new ProfitDistributionDetail();
        existing.setId(55L);
        existing.setDistribution(distribution);
        existing.setEvOwner(owner());

        when(profitDistributionDetailRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean success = service.processPayoutForOwner(
                distribution, payout, companyWallet(), 99L, null, null, 55L);

        assertThat(success).isFalse();
        ArgumentCaptor<ProfitDistributionDetail> captor =
                ArgumentCaptor.forClass(ProfitDistributionDetail.class);
        verify(profitDistributionDetailRepository, times(1)).save(captor.capture());

        ProfitDistributionDetail saved = captor.getValue();
        assertThat(saved.getId()).as("retry must update detail #55, not insert a duplicate").isEqualTo(55L);
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getErrorMessage()).contains("rollback-only");
    }

    @Test
    void infrastructureExceptionIsIsolatedPerOwnerAndRecordedAsFailed() {
        useTemplateThatNeverFailsCommit();
        when(emissionReportRepository.findByIdWithDetails(99L))
                .thenThrow(new IllegalStateException("database is gone"));
        stubFailureDetailLookups();

        ProfitDistribution distribution = distribution(7L);
        ProfitSharingServiceImpl.OwnerPayoutData payout = payout(42L);

        assertThatCode(() -> service.processPayoutForOwner(
                distribution, payout, companyWallet(), 99L, null, null, null))
                .as("one owner's infrastructure error must not abort the whole payout loop")
                .doesNotThrowAnyException();

        ArgumentCaptor<ProfitDistributionDetail> captor =
                ArgumentCaptor.forClass(ProfitDistributionDetail.class);
        verify(profitDistributionDetailRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("database is gone");
    }

    private void stubWalletCreationFailure() {
        User companyUser = new User();
        companyUser.setId(1L);
        EVOwner owner = owner();

        Wallet companyWallet = companyWallet();
        when(walletRepository.findById(companyWallet.getId())).thenReturn(Optional.of(companyWallet));
        when(evOwnerRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(walletService.findWalletByUser(owner.getUser())).thenReturn(null);
        when(walletService.generateWallet(owner.getUser()))
                .thenThrow(new IllegalStateException("wallet creation failed"));
    }

    private void stubFailureDetailLookups() {
        when(evOwnerRepository.getReferenceById(42L)).thenReturn(owner());
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void useTemplateThatFailsFirstCommit(String message) {
        AtomicInteger commits = new AtomicInteger();
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
                if (commits.getAndIncrement() == 0) {
                    throw new UnexpectedRollbackException(message);
                }
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                // no-op
            }
        });
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ReflectionTestUtils.setField(service, "requiresNewTransaction", template);
    }

    private void useTemplateThatNeverFailsCommit() {
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
        ReflectionTestUtils.setField(service, "requiresNewTransaction", template);
    }

    private Wallet companyWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(3L);
        wallet.setBalance(new BigDecimal("1000.00"));
        return wallet;
    }

    private ProfitDistribution distribution(Long id) {
        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setId(id);
        return distribution;
    }

    private EVOwner owner() {
        User user = new User();
        user.setId(42L);
        user.setEmail("owner@example.com");

        EVOwner owner = new EVOwner();
        owner.setId(42L);
        owner.setName("Owner 42");
        owner.setUser(user);
        return owner;
    }

    private ProfitSharingServiceImpl.OwnerPayoutData payout(Long ownerId) {
        return new ProfitSharingServiceImpl.OwnerPayoutData(
                ownerId,
                new BigDecimal("100.000000"),
                new BigDecimal("50.000000"),
                new BigDecimal("12.50"));
    }
}

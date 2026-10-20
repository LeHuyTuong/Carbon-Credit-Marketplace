package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.EmissionReport;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R2-3 money path: the real {@code doProcessPayoutForOwner} must move money exactly once
 * per owner.
 *
 * <p>The other payout tests stub {@code processPayoutForOwner}, so a defect inside the
 * method that actually calls {@code walletService.transferFunds} (wrong owner, wrong
 * amount, paying twice) would stay invisible. These tests drive the real method and
 * assert the transfer itself, plus the cross-distribution guard that turns a second
 * payout for the same owner into SKIPPED instead of a second transfer.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProfitSharingPayoutTransferTest {

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
                properties(),
                emailService,
                dynamicPricingService,
                transactionManager,
                new ProfitSharingTaskDispatcher(Runnable::run));
        ReflectionTestUtils.setField(service, "requiresNewTransaction", noOpTemplate());
    }

    @Test
    void ownerIsPaidOnceWithTheAgreedAmount() throws Exception {
        ProfitDistribution distribution = distribution();
        ProfitDistributionDetail detail = detail(distribution);

        Wallet companyWallet = companyWallet();
        Wallet ownerWallet = ownerWallet();
        when(profitDistributionDetailRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(detail));
        when(emissionReportRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(report()));
        when(walletRepository.findById(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(ownerWallet));
        when(evOwnerRepository.findById(42L)).thenReturn(Optional.of(owner()));
        when(walletService.findWalletByUser(any())).thenReturn(ownerWallet);
        when(profitDistributionDetailRepository.existsPaidInAnotherDistribution(99L, 42L, 7L))
                .thenReturn(false);
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean paid = service.processPayoutForOwner(
                distribution, payout(), companyWallet, 99L, policy(), company(), 55L);

        assertThat(paid).as("the owner must be paid").isTrue();
        verify(walletService).transferFunds(
                eq(companyWallet),
                eq(ownerWallet),
                eq(new BigDecimal("12.50")),
                eq("PROFIT_SHARING"),
                any(),
                any(),
                eq(distribution));
        verify(walletService, never()).generateWallet(any());
        assertThat(detail.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void ownerAlreadyPaidInAnotherDistributionIsSkippedWithoutMovingMoney() throws Exception {
        ProfitDistribution distribution = distribution();
        ProfitDistributionDetail detail = detail(distribution);

        Wallet companyWallet = companyWallet();
        Wallet ownerWallet = ownerWallet();
        when(profitDistributionDetailRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(detail));
        when(emissionReportRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(report()));
        when(walletRepository.findById(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(ownerWallet));
        when(evOwnerRepository.findById(42L)).thenReturn(Optional.of(owner()));
        when(walletService.findWalletByUser(any())).thenReturn(ownerWallet);
        when(profitDistributionDetailRepository.existsPaidInAnotherDistribution(99L, 42L, 7L))
                .thenReturn(true);
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean paid = service.processPayoutForOwner(
                distribution, payout(), companyWallet, 99L, policy(), company(), 55L);

        assertThat(paid).as("a skipped owner is not a failure").isTrue();
        verify(walletService, never()).transferFunds(any(), any(), any(), any(), any(), any(), any());
        assertThat(detail.getStatus()).isEqualTo("SKIPPED");
    }

    @Test
    void legacyDistributionWithoutReportSkipsOwnersPaidByAnotherDistributionOfTheProject() throws Exception {
        ProfitDistribution distribution = distribution();
        distribution.setEmissionReport(null);
        com.carbonx.marketcarbon.model.Project project =
                new com.carbonx.marketcarbon.model.Project();
        project.setId(20L);
        project.setTitle("Project 20");
        distribution.setProject(project);
        ProfitDistributionDetail detail = detail(distribution);

        Wallet companyWallet = companyWallet();
        Wallet ownerWallet = ownerWallet();
        when(profitDistributionDetailRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(detail));
        when(emissionReportRepository.findByIdWithDetails(null)).thenReturn(Optional.empty());
        when(walletRepository.findById(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(companyWallet));
        when(walletRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(ownerWallet));
        when(evOwnerRepository.findById(42L)).thenReturn(Optional.of(owner()));
        when(walletService.findWalletByUser(any())).thenReturn(ownerWallet);
        when(profitDistributionDetailRepository.existsPaidInAnotherDistributionOfProject(
                42L, 1L, 20L, 7L)).thenReturn(true);
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean paid = service.processPayoutForOwner(
                distribution, payout(), companyWallet, null, policy(), company(), 55L);

        assertThat(paid).isTrue();
        verify(walletService, never()).transferFunds(any(), any(), any(), any(), any(), any(), any());
        assertThat(detail.getStatus()).isEqualTo("SKIPPED");
    }

    private ProfitSharingProperties properties() {
        ProfitSharingProperties properties = new ProfitSharingProperties();
        ProfitSharingProperties.Policy policy = new ProfitSharingProperties.Policy();
        policy.setPricingMode(ProfitSharingProperties.PricingMode.CREDIT);
        policy.setOwnerSharePct(new BigDecimal("0.5"));
        properties.setDefaultPolicy(policy);
        return properties;
    }

    private ProfitSharingProperties.ResolvedPolicy policy() {
        return properties().resolveForCompany(10L);
    }

    private Company company() {
        Company company = new Company();
        company.setId(10L);
        company.setCompanyName("Acme");
        return company;
    }

    private User companyUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("company@example.com");
        return user;
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

    private EmissionReport report() {
        EmissionReport report = new EmissionReport();
        report.setId(99L);
        report.setStatus(EmissionStatus.CREDIT_ISSUED);
        report.setPeriod("2025-10");
        return report;
    }

    private ProfitDistribution distribution() {
        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setId(7L);
        distribution.setCompanyUser(companyUser());
        distribution.setStatus(ProfitDistributionStatus.PROCESSING);
        distribution.setTotalMoneyDistributed(BigDecimal.ZERO);
        distribution.setTotalCreditsDistributed(BigDecimal.ZERO);
        return distribution;
    }

    private ProfitDistributionDetail detail(ProfitDistribution distribution) {
        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setId(55L);
        detail.setDistribution(distribution);
        detail.setEvOwner(owner());
        detail.setStatus("PENDING");
        detail.setMoneyAmount(new BigDecimal("12.50"));
        detail.setCreditAmount(new BigDecimal("600.000000"));
        detail.setEnergyAmount(new BigDecimal("1000.000000"));
        return detail;
    }

    private Wallet companyWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(3L);
        wallet.setBalance(new BigDecimal("1000.00"));
        return wallet;
    }

    private Wallet ownerWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(42L);
        wallet.setBalance(BigDecimal.ZERO);
        return wallet;
    }

    private ProfitSharingServiceImpl.OwnerPayoutData payout() {
        return new ProfitSharingServiceImpl.OwnerPayoutData(
                42L,
                new BigDecimal("600.000000"),
                new BigDecimal("1000.000000"),
                new BigDecimal("12.50"));
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

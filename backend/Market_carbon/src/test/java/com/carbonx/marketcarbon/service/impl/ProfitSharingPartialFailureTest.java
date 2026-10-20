package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R3-2: a payout with at least one failed owner must not mark the report PAID_OUT and must not
 * mark the distribution COMPLETED, because that removes the retry path and tells the company
 * everyone was paid.
 */
@ExtendWith(MockitoExtension.class)
class ProfitSharingPartialFailureTest {

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
                new ProfitSharingTaskDispatcher(Runnable::run)));
        ReflectionTestUtils.setField(service, "requiresNewTransaction", noOpTemplate());
    }

    @Test
    void failedOwnerLeavesReportCreditIssuedAndDistributionFailed() {
        User companyUser = companyUser();
        Company company = company();
        EVOwner owner = owner();
        Wallet companyWallet = wallet();

        EmissionReport report = report(company, owner);
        ProfitSharingRequest request = new ProfitSharingRequest();
        request.setEmissionReportId(99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(dynamicPricingService.getMarketPricePerCredit()).thenReturn(new BigDecimal("10.00"));
        when(dynamicPricingService.getKwhPerCreditFactor()).thenReturn(new BigDecimal("1000"));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.empty());
        when(profitDistributionRepository.save(any(ProfitDistribution.class)))
                .thenAnswer(invocation -> {
                    ProfitDistribution distribution = invocation.getArgument(0);
                    if (distribution.getId() == null) {
                        distribution.setId(7L);
                    }
                    return distribution;
                });
        when(walletService.findWalletByUser(companyUser)).thenReturn(companyWallet);
        when(emissionReportRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(report));
        when(emissionReportRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(report));
        when(vehicleRepository.findByCompanyId(10L)).thenReturn(List.of(vehicle(company, owner)));
        when(walletRepository.findById(3L)).thenReturn(Optional.of(companyWallet));
        when(evOwnerRepository.getReferenceById(42L)).thenReturn(owner);

        ProfitDistribution managedDistribution = new ProfitDistribution();
        managedDistribution.setId(7L);
        when(profitDistributionRepository.findById(7L)).thenReturn(Optional.of(managedDistribution));

        ProfitDistributionDetail pending = new ProfitDistributionDetail();
        pending.setId(100L);
        pending.setDistribution(managedDistribution);
        pending.setEvOwner(owner);
        pending.setStatus("PENDING");
        pending.setMoneyAmount(new BigDecimal("3.00"));
        pending.setCreditAmount(new BigDecimal("600.000000"));
        pending.setEnergyAmount(new BigDecimal("1000.000000"));
        when(profitDistributionDetailRepository.save(any(ProfitDistributionDetail.class)))
                .thenAnswer(invocation -> {
                    ProfitDistributionDetail detail = invocation.getArgument(0);
                    if (detail.getId() == null) {
                        detail.setId(100L);
                    }
                    return detail;
                });
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L))
                .thenReturn(List.of(pending));
        when(profitDistributionRepository.updateTotalsIfClaimed(
                eq(7L), any(), any(), anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), eq(ProfitDistributionStatus.FAILED),
                eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);

        // Owner payout fails in the per-owner transaction.
        doReturn(false).when(service).processPayoutForOwner(
                any(ProfitDistribution.class), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                any(Wallet.class), any(), any(), any(), any());

        service.shareCompanyProfit(1L, request);

        verify(emissionReportRepository, never()).save(any());
        // /share must heartbeat during its payout loop, not only the retry path.
        verify(profitDistributionRepository).touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString());
        // The distribution is failed through the claim-fenced CAS, not save(entity).
        verify(profitDistributionRepository).finishIfClaimed(
                eq(7L), eq(ProfitDistributionStatus.FAILED),
                eq(ProfitDistributionStatus.PROCESSING), anyString(), any());
        verify(emailService, never()).sendDistributionSummaryToCompany(
                any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyBoolean(), any(), any());
    }

    @Test
    void retryPaysOnlyFailedDetailsAndCompletesReportWhenAllSucceed() {
        User companyUser = companyUser();
        Company company = company();
        EVOwner owner = owner();
        Wallet companyWallet = wallet();
        EmissionReport report = report(company, owner);

        ProfitDistribution distribution = new ProfitDistribution();
        distribution.setId(7L);
        distribution.setCompanyUser(companyUser);
        distribution.setEmissionReport(report);
        distribution.setStatus(ProfitDistributionStatus.FAILED);

        ProfitDistributionDetail failed = new ProfitDistributionDetail();
        failed.setId(55L);
        failed.setDistribution(distribution);
        failed.setEvOwner(owner);
        failed.setStatus("FAILED");
        failed.setMoneyAmount(new BigDecimal("3.00"));
        failed.setCreditAmount(new BigDecimal("600.000000"));
        failed.setEnergyAmount(new BigDecimal("1000.000000"));

        ProfitDistributionDetail alreadyPaid = new ProfitDistributionDetail();
        alreadyPaid.setId(56L);
        alreadyPaid.setDistribution(distribution);
        alreadyPaid.setEvOwner(owner);
        alreadyPaid.setStatus("SUCCESS");
        alreadyPaid.setMoneyAmount(new BigDecimal("2.00"));
        alreadyPaid.setCreditAmount(new BigDecimal("400.000000"));
        alreadyPaid.setEnergyAmount(new BigDecimal("800.000000"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(companyUser));
        when(companyRepository.findByUserId(1L)).thenReturn(Optional.of(company));
        when(profitDistributionRepository.findWithReportById(7L)).thenReturn(Optional.of(distribution));
        when(profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(99L))
                .thenReturn(Optional.of(distribution));
        when(profitDistributionDetailRepository.countByDistributionId(7L)).thenReturn(1L);
        when(profitDistributionRepository.markRetrying(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), eq(ProfitDistributionStatus.FAILED),
                anyString(), any())).thenReturn(1);
        when(profitDistributionRepository.touchProcessing(
                eq(7L), eq(ProfitDistributionStatus.PROCESSING), any(), anyString())).thenReturn(1);
        when(profitDistributionRepository.finishIfClaimed(
                eq(7L), eq(ProfitDistributionStatus.COMPLETED),
                eq(ProfitDistributionStatus.PROCESSING), anyString(), any())).thenReturn(1);
        when(profitDistributionDetailRepository.findByDistributionIdWithOwner(7L))
                .thenReturn(List.of(failed, alreadyPaid));
        when(walletService.findWalletByUser(companyUser)).thenReturn(companyWallet);
        when(walletRepository.findById(3L)).thenReturn(Optional.of(companyWallet));
        when(emissionReportRepository.findById(99L)).thenReturn(Optional.of(report));
        when(emissionReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            failed.setStatus("SUCCESS");
            return true;
        }).when(service).processPayoutForOwner(
                eq(distribution), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(companyWallet), eq(99L), any(), eq(company), eq(55L));

        service.requestRetry(1L, 7L);

        // Only detail #55 (FAILED) is retried; #56 (SUCCESS) is left untouched.
        verify(service, org.mockito.Mockito.times(1)).processPayoutForOwner(
                eq(distribution), any(ProfitSharingServiceImpl.OwnerPayoutData.class),
                eq(companyWallet), eq(99L), any(), eq(company), eq(55L));
        verify(emissionReportRepository, org.mockito.Mockito.times(1)).save(report);
        assertThat(report.getStatus()).isEqualTo(EmissionStatus.PAID_OUT);
        assertThat(distribution.getStatus()).isEqualTo(ProfitDistributionStatus.COMPLETED);
    }

    private ProfitSharingProperties properties() {
        ProfitSharingProperties properties = new ProfitSharingProperties();
        ProfitSharingProperties.Policy policy = new ProfitSharingProperties.Policy();
        policy.setPricingMode(ProfitSharingProperties.PricingMode.CREDIT);
        policy.setOwnerSharePct(new BigDecimal("0.5"));
        properties.setDefaultPolicy(policy);
        return properties;
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

    private User companyUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("company@example.com");
        return user;
    }

    private Company company() {
        Company company = new Company();
        company.setId(10L);
        company.setCompanyName("Acme");
        return company;
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

    private Wallet wallet() {
        Wallet wallet = new Wallet();
        wallet.setId(3L);
        wallet.setBalance(new BigDecimal("1000.00"));
        return wallet;
    }

    private Vehicle vehicle(Company company, EVOwner owner) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("ABC");
        vehicle.setCompany(company);
        vehicle.setEvOwner(owner);
        return vehicle;
    }

    private EmissionReport report(Company company, EVOwner owner) {
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Project 20");

        EmissionReportDetail detail = new EmissionReportDetail();
        detail.setCompanyId(10L);
        detail.setProjectId(20L);
        detail.setTotalEnergy(new BigDecimal("1000.0000"));
        detail.setCo2Kg(new BigDecimal("600.0000"));
        detail.setVehiclePlate("ABC");

        EmissionReport report = new EmissionReport();
        report.setId(99L);
        report.setSeller(company);
        report.setProject(project);
        report.setPeriod("2025-10");
        report.setStatus(EmissionStatus.CREDIT_ISSUED);
        report.setDetails(List.of(detail));
        return report;
    }
}

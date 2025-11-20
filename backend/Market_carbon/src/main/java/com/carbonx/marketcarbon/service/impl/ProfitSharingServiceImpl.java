package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;
import com.carbonx.marketcarbon.config.ProfitSharingProperties.ResolvedPolicy;
import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service chịu trách nhiệm phân chia lợi nhuận (Payout) cho các chủ xe điện (EV Owners).
 * Logic bao gồm: Tính toán đóng góp, trừ tiền ví công ty, cộng tiền ví Owner, và gửi email thông báo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProfitSharingServiceImpl implements ProfitSharingService {

    private static final BigDecimal DEFAULT_EMISSION_FACTOR = new BigDecimal("0.6");

    private final EmissionReportRepository emissionReportRepository;
    private final VehicleRepository vehicleRepository;
    private final EVOwnerRepository evOwnerRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final ProfitDistributionDetailRepository profitDistributionDetailRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final ProfitSharingProperties profitSharingProperties;
    private final EmailService emailService;
    private final DynamicPricingService dynamicPricingService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Helper để lấy instance của chính bean này từ ApplicationContext.
     * Mục đích: Để gọi các method có @Transactional (như processPayoutForOwner) từ bên trong cùng class
     * mà vẫn kích hoạt được Spring AOP Proxy (đảm bảo Transaction hoạt động đúng).
     */
    private ProfitSharingServiceImpl getSelf() {
        return applicationContext.getBean(ProfitSharingServiceImpl.class);
    }

    // DTO nội bộ dùng để cộng dồn năng lượng và tín chỉ cho từng Owner
    @Data
    static class ContributionData {
        private final Long evOwnerId;
        private BigDecimal totalEnergyContribution = BigDecimal.ZERO;
        private BigDecimal totalCreditContribution = BigDecimal.ZERO;

        public ContributionData(Long evOwnerId) {
            this.evOwnerId = evOwnerId;
        }

        public void addContribution(BigDecimal energy, BigDecimal credit) {
            if (energy != null && energy.compareTo(BigDecimal.ZERO) > 0) {
                this.totalEnergyContribution = this.totalEnergyContribution.add(energy);
            }
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                this.totalCreditContribution = this.totalCreditContribution.add(credit);
            }
        }
    }

    // DTO nội bộ dùng để lưu kế hoạch chi trả sau khi đã tính toán giá tiền
    @Data
    static class OwnerPayoutData {
        private final Long evOwnerId;
        private final BigDecimal creditContribution;
        private final BigDecimal energyContribution;
        private final BigDecimal rawPayoutAmount;
        private BigDecimal payoutAmount;

        private OwnerPayoutData(Long evOwnerId,
                                BigDecimal creditContribution,
                                BigDecimal energyContribution,
                                BigDecimal rawPayoutAmount) {
            this.evOwnerId = evOwnerId;
            this.creditContribution = creditContribution;
            this.energyContribution = energyContribution;
            // Làm tròn tiền tệ đến 2 chữ số thập phân ngay khi khởi tạo
            this.rawPayoutAmount = rawPayoutAmount.setScale(2, RoundingMode.HALF_UP);
            this.payoutAmount = this.rawPayoutAmount;
        }
    }

    /**
     * Hàm xử lý chính: Chia sẻ lợi nhuận.
     * Chạy ASYNC (bất đồng bộ) để không block luồng chính của user khi xử lý danh sách lớn.
     */
    @Async("profitSharingTaskExecutor")
    @Override
    public void shareCompanyProfit(ProfitSharingRequest request) {
        // B1: Xác thực user hiện tại (Company Admin)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User companyUser = userRepository.findByEmail(authentication.getName());
        if (companyUser == null) {
            log.error("Authentication failed, user not found for email: {}", authentication.getName());
            return;
        }

        Company company = companyRepository.findByUserId(companyUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        // B2: Lấy cấu hình Policy và Giá thị trường (Dynamic Pricing)
        ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
        BigDecimal marketPricePerCredit = dynamicPricingService.getMarketPricePerCredit();
        BigDecimal kwhPerCreditFactor = dynamicPricingService.getKwhPerCreditFactor();

        // Tính giá payout thực tế cho Owner (Giá thị trường * % chia sẻ)
        BigDecimal actualPayoutPricePerCredit = marketPricePerCredit
                .multiply(policy.getOwnerSharePct())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Execution Policy for companyId={}: source={}, pricingMode={}, ownerSharePct={}",
                company.getId(), policy.getSource(), policy.getPricingMode(), policy.getOwnerSharePct());

        log.info("Processing to share profit by company : {}", companyUser.getEmail());

        // B3: Tạo bản ghi theo dõi đợt phân phối (Status: PROCESSING)
        ProfitDistribution distributionEvent = getSelf().createDistributionEvent(request, companyUser);

        try {
            Wallet companyWallet = walletService.findWalletByUser(companyUser);
            List<EmissionReport> reportsToProcess = new ArrayList<>();

            // Biến report dùng chung cho xử lý và email
            EmissionReport report;

            // B4: Validate và Load Emission Report
            if (request.getEmissionReportId() != null) {
                // Dùng findByIdWithDetails để fetch EAGER các data cần thiết
                report = emissionReportRepository.findByIdWithDetails(request.getEmissionReportId())
                        .orElseThrow(() -> new BadRequestException("No find emission report with id : " + request.getEmissionReportId()));

                // Chỉ xử lý report đã được cấp tín chỉ (CREDIT_ISSUED)
                if (report.getStatus() != EmissionStatus.CREDIT_ISSUED ) {
                    throw new AppException(ErrorCode.EMISSION_REPORT_NOT_APPROVED);
                }
                if (!Objects.equals(report.getSeller().getId(), company.getId())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
                reportsToProcess.add(report);
            } else {
                log.warn("No emissionReportId provided.");
                markDistributionCompleted(distributionEvent);
                return;
            }

            if (reportsToProcess.isEmpty()) {
                markDistributionCompleted(distributionEvent);
                return;
            }

            // B5: Chuẩn bị Map mapping từ Biển số xe -> Vehicle Entity để tra cứu nhanh
            Map<String, Vehicle> vehicleByPlate = buildCompanyVehicleMap(company.getId());
            if (vehicleByPlate.isEmpty()) {
                markDistributionCompleted(distributionEvent);
                return;
            }

            // B6: Vòng lặp tổng hợp dữ liệu (Aggregation)
            Map<Long, ContributionData> evOwnerContributions = new HashMap<>();

            for (EmissionReport r : reportsToProcess) {
                for (EmissionReportDetail detail : r.getDetails()) {
                    // Bỏ qua nếu detail không thuộc company này
                    if (!Objects.equals(detail.getCompanyId(), company.getId())) {
                        continue;
                    }
                    BigDecimal energy = normalizeAmount(detail.getTotalEnergy(), 6);
                    if (energy.compareTo(BigDecimal.ZERO) <= 0) continue;

                    // Tìm xe và Owner dựa trên biển số
                    Vehicle vehicle = vehicleByPlate.get(normalizePlate(detail.getVehiclePlate()));
                    if (vehicle == null || vehicle.getEvOwner() == null) continue;

                    // Tính lượng Credit đóng góp
                    BigDecimal creditContributionKg = resolveCreditContribution(detail, energy);

                    // Cộng dồn vào Map
                    ContributionData contribution = evOwnerContributions.computeIfAbsent(
                            vehicle.getEvOwner().getId(), ContributionData::new);
                    contribution.addContribution(energy, creditContributionKg);
                }
            }

            if (evOwnerContributions.isEmpty()) {
                log.warn("No eligible vehicle contributions found for company {}", company.getId());
                markDistributionCompleted(distributionEvent);
                return;
            }

            // B7: Tính toán số tiền cần trả (Payout Plan)
            List<OwnerPayoutData> payoutPlan = new ArrayList<>();
            BigDecimal totalRawPayout = BigDecimal.ZERO;
            BigDecimal totalCreditsKgForDistribution = BigDecimal.ZERO;
            BigDecimal totalEnergyForDistribution = BigDecimal.ZERO;

            for (ContributionData contribution : evOwnerContributions.values()) {
                // Tính tiền dựa trên công thức (KWH hoặc CREDIT)
                BigDecimal rawPayout = calculateRawPayout(
                        contribution,
                        policy,
                        actualPayoutPricePerCredit,
                        kwhPerCreditFactor
                );

                if (rawPayout.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Kiểm tra mức chi trả tối thiểu (Min Payout)
                if (policy.getMinPayout() != null && rawPayout.compareTo(policy.getMinPayout()) < 0) {
                    log.info("Skipping payout for owner {} due to min payout threshold", contribution.getEvOwnerId());
                    continue;
                }

                BigDecimal creditContributionKg = normalizeAmount(contribution.getTotalCreditContribution(), 6);
                BigDecimal energyContribution = normalizeAmount(contribution.getTotalEnergyContribution(), 6);

                payoutPlan.add(new OwnerPayoutData(
                        contribution.getEvOwnerId(),
                        creditContributionKg,
                        energyContribution,
                        rawPayout));

                totalRawPayout = totalRawPayout.add(rawPayout);
                totalCreditsKgForDistribution = totalCreditsKgForDistribution.add(creditContributionKg);
                totalEnergyForDistribution = totalEnergyForDistribution.add(energyContribution);
            }

            if (payoutPlan.isEmpty() || totalRawPayout.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Computed payout amount is zero.");
                markDistributionCompleted(distributionEvent);
                return;
            }

            BigDecimal finalTotal = totalRawPayout.setScale(2, RoundingMode.HALF_UP);

            // B8: Kiểm tra số dư ví công ty trước khi thực hiện
            validateCompanyBalance(companyWallet, finalTotal);

            // Update thông tin tổng vào sự kiện phân phối
            distributionEvent.setTotalMoneyDistributed(finalTotal);
            distributionEvent.setTotalCreditsDistributed(totalCreditsKgForDistribution.setScale(6, RoundingMode.HALF_UP));
            profitDistributionRepository.save(distributionEvent);

            log.info("Starting sequential payout for {} owners...", payoutPlan.size());

            // B9: Thực hiện chuyển tiền tuần tự cho từng Owner
            // Gọi qua getSelf() để đảm bảo Transactional (REQUIRES_NEW) hoạt động
            for (OwnerPayoutData payout : payoutPlan) {
                getSelf().processPayoutForOwner(
                        distributionEvent,
                        payout,
                        companyWallet,
                        report.getId(), // Chỉ truyền ID để load lại trong transaction con
                        policy,
                        company
                );
            }

            log.info("All sequential payouts processed.");

            // B10: Cập nhật trạng thái Report -> PAID_OUT
            getSelf().updateReportsToPaidOut(reportsToProcess);

            // B11: Gửi email tổng kết cho Công ty
            try {
                BigDecimal totalCreditsTCO2e = totalCreditsKgForDistribution.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                String period = report.getPeriod(); // Safe access vì report đã load EAGER ở trên

                emailService.sendDistributionSummaryToCompany(
                        companyUser.getEmail(),
                        company.getCompanyName(),
                        period,
                        payoutPlan.size(),
                        totalEnergyForDistribution,
                        totalCreditsTCO2e,
                        finalTotal,
                        false,
                        company.getId(),
                        String.valueOf(distributionEvent.getId())
                );
            } catch (Exception e) {
                log.warn("Failed to send payout summary email: {}", e.getMessage());
            }

            // Hoàn tất quy trình
            markDistributionCompleted(distributionEvent);

        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage(), e);
            distributionEvent.setStatus(ProfitDistributionStatus.FAILED);
            profitDistributionRepository.save(distributionEvent);
        }
    }

    /**
     * Xử lý chuyển tiền cho MỘT Owner cụ thể.
     * IMPORTANT: Sử dụng Propagation.REQUIRES_NEW để tạo một transaction hoàn toàn mới.
     * Nếu transaction này fail (ví dụ lỗi mạng, lỗi ví), nó chỉ rollback cho Owner này,
     * KHÔNG ảnh hưởng đến các Owner khác đã được trả tiền.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processPayoutForOwner(
            ProfitDistribution event,
            OwnerPayoutData payout,
            Wallet companyWallet,
            Long reportId,
            ResolvedPolicy policy,
            Company company
    ) {
        // Load lại Report trong session mới để tránh LazyInitializationException
        EmissionReport report = emissionReportRepository.findByIdWithDetails(reportId).orElse(null);

        String projectName = "N/A";
        String reportPeriod = "N/A";
        String reportIdStr = String.valueOf(reportId);

        if (report != null) {
            try {
                if (report.getProject() != null) projectName = report.getProject().getTitle();
                reportPeriod = report.getPeriod();
            } catch (Exception e) {
                log.warn("Could not eager fetch report data: {}", e.getMessage());
            }
        }

        // Load lại ví Company để đảm bảo thread-safe và số dư mới nhất
        Wallet threadSafeCompanyWallet = walletRepository.findById(companyWallet.getId()).orElse(null);
        if (threadSafeCompanyWallet == null) {
            getSelf().saveFailedDetail(event, payout, "Company wallet not found");
            return;
        }

        EVOwner owner = evOwnerRepository.findById(payout.getEvOwnerId()).orElse(null);
        if (owner == null || owner.getUser() == null) {
            getSelf().saveFailedDetail(event, payout, "EVOwner not found");
            return;
        }

        // Tìm hoặc Tự động tạo ví cho Owner nếu chưa có
        Wallet ownerWallet = walletService.findWalletByUser(owner.getUser());
        if (ownerWallet == null) {
            try {
                ownerWallet = ((WalletServiceImpl) walletService).generateWallet(owner.getUser());
                log.info("Generated new wallet for EVOwner ID {}", payout.getEvOwnerId());
            } catch (Exception e) {
                getSelf().saveFailedDetail(event, payout, "Failed to create wallet: " + e.getMessage());
                return;
            }
        }

        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setDistribution(event);
        detail.setEvOwner(owner);
        detail.setMoneyAmount(payout.getPayoutAmount());
        detail.setCreditAmount(payout.getCreditContribution());
        detail.setEnergyAmount(payout.getEnergyContribution());

        try {
            // 1. Thực hiện chuyển tiền (Core Logic)
            if (payout.getPayoutAmount().compareTo(BigDecimal.ZERO) > 0) {
                walletService.transferFunds(
                        threadSafeCompanyWallet,
                        ownerWallet,
                        payout.getPayoutAmount(),
                        WalletTransactionType.PROFIT_SHARING.name(),
                        String.format("Sharing profit to EV owner %s (distribution #%d)", owner.getName(), event.getId()),
                        String.format("Profit-sharing from distribution #%d", event.getId()),
                        event
                );
            }

            // 2. Gửi email thông báo tiền về (Non-blocking logic ideally, but here it's sequential)
            try {
                String reportReference = "Report #" + reportIdStr + " (" + projectName + ")";
                BigDecimal creditsAsTCO2e = payout.getCreditContribution().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);

                emailService.sendPayoutSuccessToOwner(
                        owner.getUser().getEmail(),
                        owner.getName(),
                        company.getCompanyName(),
                        reportPeriod,
                        payout.getEnergyContribution(),
                        creditsAsTCO2e,
                        payout.getPayoutAmount(),
                        java.util.Collections.emptyList(),
                        String.valueOf(event.getId()),
                        company.getId(),
                        reportReference,
                        policy.getMinPayout()
                );
            } catch (Exception e) {
                // Email lỗi không nên làm rollback việc chuyển tiền
                log.warn("Failed to send payout success email to owner {}: {}", owner.getId(), e.getMessage());
            }

            detail.setStatus("SUCCESS");

        } catch (Exception e) {
            // Nếu chuyển tiền lỗi -> Save trạng thái FAILED
            log.error("Exception during payout for EVOwner ID {}: {}", payout.getEvOwnerId(), e.getMessage());
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            detail.setStatus("FAILED");
            detail.setErrorMessage(errorMessage.substring(0, Math.min(errorMessage.length(), 250)));
        } finally {
            // Luôn lưu log chi tiết giao dịch
            profitDistributionDetailRepository.save(detail);
        }
    }

    // Tạo bản ghi sự kiện phân phối trong transaction riêng
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ProfitDistribution createDistributionEvent(ProfitSharingRequest request, User companyUser) {
        ProfitDistribution event = new ProfitDistribution();
        event.setCompanyUser(companyUser);
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new BadRequestException("Project ID not found: " + request.getProjectId()));
            event.setProject(project);
        }
        event.setTotalMoneyDistributed(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        event.setTotalCreditsDistributed(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        event.setStatus(ProfitDistributionStatus.PROCESSING);
        return profitDistributionRepository.save(event);
    }

    // Lưu log lỗi khi không thể bắt đầu process payout (vd: ko tìm thấy user)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedDetail(ProfitDistribution event, OwnerPayoutData payout, String errorMessage) {
        try {
            EVOwner ownerRef = evOwnerRepository.getReferenceById(payout.getEvOwnerId());
            ProfitDistributionDetail detail = new ProfitDistributionDetail();
            detail.setDistribution(event);
            detail.setEvOwner(ownerRef);
            detail.setMoneyAmount(payout.getPayoutAmount());
            detail.setCreditAmount(payout.getCreditContribution());
            detail.setEnergyAmount(payout.getEnergyContribution());
            detail.setStatus("FAILED");
            detail.setErrorMessage(errorMessage.substring(0, Math.min(errorMessage.length(), 254)));
            profitDistributionDetailRepository.save(detail);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to save failure detail for owner {}: {}", payout.getEvOwnerId(), e.getMessage());
        }
    }

    // Helper map biển số xe
    private Map<String, Vehicle> buildCompanyVehicleMap(Long companyId) {
        List<Vehicle> vehicles = vehicleRepository.findByCompanyId(companyId);
        Map<String, Vehicle> vehicleMap = new HashMap<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getEvOwner() == null) continue;
            String normalizedPlate = normalizePlate(vehicle.getPlateNumber());
            if (normalizedPlate != null && !normalizedPlate.isEmpty()) {
                vehicleMap.put(normalizedPlate, vehicle);
            }
        }
        return vehicleMap;
    }

    // Update status report trong transaction riêng
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReportsToPaidOut(List<EmissionReport> reports) {
        if (reports == null || reports.isEmpty()) return;
        List<Long> reportIds = reports.stream().map(EmissionReport::getId).toList();
        List<EmissionReport> freshReports = emissionReportRepository.findAllById(reportIds);
        for (EmissionReport report : freshReports) {
            report.setStatus(EmissionStatus.PAID_OUT);
        }
        emissionReportRepository.saveAll(freshReports);
    }

    private void markDistributionCompleted(ProfitDistribution event) {
        event.setStatus(ProfitDistributionStatus.COMPLETED);
        profitDistributionRepository.save(event);
    }

    // Tính toán tiền payout dựa trên Pricing Mode
    private BigDecimal calculateRawPayout(
            ContributionData contribution,
            ResolvedPolicy policy,
            BigDecimal actualPayoutPricePerCredit,
            BigDecimal kwhPerCreditFactor
    ) {
        // Case 1: Tính theo kWh (Năng lượng sạc)
        if (policy.getPricingMode() == PricingMode.KWH) {
            if (kwhPerCreditFactor.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("KWH_PER_CREDIT_FACTOR is zero, defaulting to CREDIT mode.");
            } else {
                // Quy đổi giá 1 Credit -> giá 1 kWh
                BigDecimal payoutPricePerKwh = actualPayoutPricePerCredit.divide(kwhPerCreditFactor, 6, RoundingMode.HALF_UP);
                return contribution.getTotalEnergyContribution()
                        .multiply(payoutPricePerKwh)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // Case 2: Tính theo Credit (Mặc định) - Quy đổi kg -> Tấn (Credit) -> Tiền
        BigDecimal creditsAsTCO2e = contribution.getTotalCreditContribution()
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);

        return creditsAsTCO2e
                .multiply(actualPayoutPricePerCredit)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal value, int scale) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    private String normalizePlate(String plate) {
        return plate == null ? null : plate.replaceAll("\\s+", "").toUpperCase();
    }

    // Kiểm tra số dư ví trước khi chạy job
    private void validateCompanyBalance(Wallet companyWallet, BigDecimal requiredAmount) throws WalletException {
        if (requiredAmount == null) return;

        Wallet freshWallet = walletRepository.findById(companyWallet.getId())
                .orElseThrow(() -> new WalletException("Wallet not found during validation"));

        if (freshWallet.getBalance().compareTo(requiredAmount) < 0) {
            log.warn("Insufficient funds: Wallet {} has {} but requires {}",
                    freshWallet.getId(), freshWallet.getBalance(), requiredAmount);
            throw new AppException(ErrorCode.WALLET_INSUFFICIENT_FUNDS);
        }
    }

    private BigDecimal resolveCreditContribution(EmissionReportDetail detail, BigDecimal fallbackEnergy) {
        if (detail == null) return BigDecimal.ZERO;
        // Ưu tiên lấy CO2 tính sẵn
        BigDecimal co2Kg = detail.getCo2Kg();
        if (co2Kg != null && co2Kg.compareTo(BigDecimal.ZERO) > 0) {
            return co2Kg.setScale(6, RoundingMode.HALF_UP);
        }
        // Fallback: Tính từ Energy * Hệ số mặc định
        if (fallbackEnergy != null && fallbackEnergy.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackEnergy.multiply(DEFAULT_EMISSION_FACTOR).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }
}

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus; // THAY ĐỔI
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
import com.carbonx.marketcarbon.service.ProfitSharingService;
import com.carbonx.marketcarbon.service.WalletService;
import lombok.Data; // THÊM
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    @Data
    private static class ContributionData {
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

    @Data
    private static class VehicleSnapshot {
        private final Long id;
        private final String plateNumber;
        private final String brand;
        private final String model;
        private final Long evOwnerId;

        private VehicleSnapshot(Vehicle vehicle) {
            this.id = vehicle.getId();
            this.plateNumber = vehicle.getPlateNumber();
            this.brand = vehicle.getBrand();
            this.model = vehicle.getModel();
            this.evOwnerId = vehicle.getEvOwner() != null ? vehicle.getEvOwner().getId() : null;
        }
    }

    /**
     * DTO nội bộ để tổng hợp đóng góp của mỗi chủ xe.
     */
    @Data
    private static class OwnerPayoutData {
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
            this.rawPayoutAmount = rawPayoutAmount.setScale(2, RoundingMode.HALF_UP);
            this.payoutAmount = this.rawPayoutAmount;
        }
    }


    @Async("profitSharingTaskExecutor") // Chạy bất đồng bộ trên luồng riêng
    @Override
    public void shareCompanyProfit(ProfitSharingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User companyUser = userRepository.findByEmail(authentication.getName());
        if (companyUser == null) {
            log.error("Authentication failed, user not found for email: {}", authentication.getName());
            return; // Không ném lỗi ra ngoài @Async
        }

        Company company = companyRepository.findByUserId(companyUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());

        log.info("Processing to share profit by company : {}", companyUser.getEmail());
        // 1. Tạo và lưu sự kiện chia lợi nhuận
        ProfitDistribution distributionEvent = createDistributionEvent(request, companyUser);

        try {
            // 2. Lấy ví của công ty (Không nằm trong TX)
            Wallet companyWallet = walletService.findWalletByUser(companyUser);

            // 3. Lấy danh sách EmissionReport
            List<EmissionReport> reportsToProcess = new ArrayList<>();

            // Công ty muốn chia cho 1 report cụ thể
            if (request.getEmissionReportId() != null) {

                // sử dụng detail để tránh lỗi lazy init
                EmissionReport report = emissionReportRepository.findByIdWithDetails(request.getEmissionReportId())
                        .orElseThrow(() -> new BadRequestException("No find emission report with id : " + request.getEmissionReportId()));

                if (report.getStatus() != EmissionStatus.CREDIT_ISSUED ) {
                    throw new AppException(ErrorCode.EMISSION_REPORT_NOT_APPROVED);
                }
                if (!Objects.equals(report.getSeller().getId(), company.getId())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
                reportsToProcess.add(report);
            }

            if (reportsToProcess.isEmpty()) {
                log.warn("Do not have emission report is approved to share profit.");
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED); // Hoàn thành (không có gì để làm)
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            Map<String, Vehicle> vehicleByPlate = buildCompanyVehicleMap(company.getId());
            if (vehicleByPlate.isEmpty()) {
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            Map<Long, ContributionData> evOwnerContributions = new HashMap<>();
            BigDecimal totalCreditsForDistribution = BigDecimal.ZERO;

            for (EmissionReport report : reportsToProcess) {
                for (EmissionReportDetail detail : report.getDetails()) {
                    if (!Objects.equals(detail.getCompanyId(), company.getId())) {
                        continue;
                    }
                    BigDecimal energy = normalizeAmount(detail.getTotalEnergy(), 6);
                    if (energy.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    Vehicle vehicle = vehicleByPlate.get(normalizePlate(detail.getVehiclePlate()));
                    if (vehicle == null || vehicle.getEvOwner() == null) {
                        continue;
                    }

                    BigDecimal creditContribution = resolveCreditContribution(detail, energy);
                    ContributionData contribution = evOwnerContributions.computeIfAbsent(
                            vehicle.getEvOwner().getId(), ContributionData::new);
                    contribution.addContribution(energy, creditContribution);
                }
            }

            if (evOwnerContributions.isEmpty()) {
                log.warn("No eligible vehicle contributions found for company {}", company.getId());
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            List<OwnerPayoutData> payoutPlan = new ArrayList<>();
            BigDecimal totalRawPayout = BigDecimal.ZERO;

            for (ContributionData contribution : evOwnerContributions.values()) {
                // TÍNH TOÁN PAYOUT CHUẨN (KHÔNG CHIA %)
                BigDecimal rawPayout = calculateRawPayout(contribution, policy);
                if (rawPayout.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (policy.getMinPayout() != null && rawPayout.compareTo(policy.getMinPayout()) < 0) {
                    log.info("Skipping payout for owner {} due to min payout threshold {}", contribution.getEvOwnerId(), policy.getMinPayout());
                    continue;
                }
                BigDecimal creditContribution = normalizeAmount(contribution.getTotalCreditContribution(), 6);
                BigDecimal energyContribution = normalizeAmount(contribution.getTotalEnergyContribution(), 6);
                payoutPlan.add(new OwnerPayoutData(
                        contribution.getEvOwnerId(),
                        creditContribution,
                        energyContribution,
                        rawPayout));
                totalRawPayout = totalRawPayout.add(rawPayout);
                totalCreditsForDistribution = totalCreditsForDistribution.add(creditContribution);
            }

            if (payoutPlan.isEmpty() || totalRawPayout.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Computed payout amount is zero after applying policy filters.");
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            // TỔNG TIỀN TRẢ = TỔNG PAYOUT (KHÔNG CÓ CAP)
            BigDecimal finalTotal = totalRawPayout.setScale(2, RoundingMode.HALF_UP);

            // KIỂM TRA VÍ CÔNG TY VỚI TỔNG SỐ TIỀN PHẢI TRẢ
            validateCompanyBalance(companyWallet, finalTotal);

            // LƯU TỔNG SỐ TIỀN PHẢI TRẢ
            distributionEvent.setTotalMoneyDistributed(finalTotal);
            distributionEvent.setTotalCreditsDistributed(totalCreditsForDistribution.setScale(6, RoundingMode.HALF_UP));

            profitDistributionRepository.save(distributionEvent);

            log.info("Starting sequential payout for {} owners...", payoutPlan.size());
            for (OwnerPayoutData payout : payoutPlan) {
                // THỰC THI CHUYỂN TIỀN
                processPayoutForOwner(distributionEvent, payout, companyWallet);
            }

            log.info("All sequential payouts processed.");

            updateReportsToPaidOut(reportsToProcess);

            distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
            profitDistributionRepository.save(distributionEvent);

        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage(), e);
            String errorMessage = (e instanceof AppException ae) ? ae.getErrorCode().getMessage() : e.getMessage();

            distributionEvent.setStatus(ProfitDistributionStatus.FAILED);

            profitDistributionRepository.save(distributionEvent);
        }
    }


    /**
     * BƯỚC 3: Xử lý thanh toán (Hàm này tạo Lịch sử Giao dịch)
     * Hàm này chạy trong 1 GIAO DỊCH MỚI (REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processPayoutForOwner(ProfitDistribution event, OwnerPayoutData payout, Wallet companyWallet) {
        // Lấy lại (fresh) company wallet trong transaction mới để tránh lỗi "detached entity"
        // và đảm bảo số dư là mới nhất.
        Wallet threadSafeCompanyWallet = walletRepository.findById(companyWallet.getId()).orElse(null);
        if (threadSafeCompanyWallet == null) {
            log.error("Company wallet not found in async thread! Wallet ID: {}", companyWallet.getId());
            saveFailedDetail(event, payout, "Company wallet not found");
            return;
        }

        EVOwner owner = evOwnerRepository.findById(payout.getEvOwnerId()).orElse(null);
        if (owner == null || owner.getUser() == null) {
            log.error("EVOwner or associated User not found for EVOwner ID: {}", payout.getEvOwnerId());
            saveFailedDetail(event, payout, "EVOwner not found");
            return;
        }

        Wallet ownerWallet = walletService.findWalletByUser(owner.getUser());
        if (ownerWallet == null) {
            // Tự động tạo ví nếu chưa có
            try {
                ownerWallet = ((WalletServiceImpl) walletService).generateWallet(owner.getUser());
                log.info("Wallet for EVOwner ID {} not found. Generated new wallet.", payout.getEvOwnerId());
            } catch (Exception e) {
                log.error("Failed to generate wallet for EVOwner ID {}: {}", payout.getEvOwnerId(), e.getMessage());
                saveFailedDetail(event, payout, "Failed to create wallet: " + e.getMessage());
                return;
            }
        }

        // tạo detail profit sharing
        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setDistribution(event);
        detail.setEvOwner(owner);
        detail.setMoneyAmount(payout.getPayoutAmount()); // SỐ TIỀN CHUẨN
        detail.setCreditAmount(payout.getCreditContribution());
        detail.setEnergyAmount(payout.getEnergyContribution());

        try {
            if (payout.getPayoutAmount().compareTo(BigDecimal.ZERO) > 0) {
                // CHUYỂN TIỀN CHUẨN
                walletService.transferFunds(
                        threadSafeCompanyWallet,
                        ownerWallet,
                        payout.getPayoutAmount(),
                        WalletTransactionType.PROFIT_SHARING.name(),
                        String.format("Sharing profit to EV owner %s (distribution #%d)", owner.getName(), event.getId()),
                        String.format("Profit-sharing from distribution #%d", event.getId()),event
                );
            }
            detail.setStatus("SUCCESS");
            log.info("Successfully processed payout for EVOwner ID: {}", payout.getEvOwnerId());

        } catch (Exception e) {
            log.error("Exception during payout for EVOwner ID {}: {}", payout.getEvOwnerId(), e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage == null) errorMessage = "Unknown error";
            if (errorMessage.length() > 250) {
                errorMessage = errorMessage.substring(0, 250) + "...";
            }
            detail.setStatus("FAILED");
            detail.setErrorMessage(errorMessage);
        } finally {
            profitDistributionDetailRepository.save(detail);
        }
    }


    /**
     * BƯỚC 1: Tạo sự kiện (Hàm này tạo Lịch sử Giao dịch)
     * Hàm này chạy trong 1 GIAO DỊCH MỚI (REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ProfitDistribution createDistributionEvent(ProfitSharingRequest request, User companyUser) {
        ProfitDistribution event = new ProfitDistribution();
        event.setCompanyUser(companyUser);
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy Project với ID: " + request.getProjectId()));
            event.setProject(project);
        }
        event.setTotalMoneyDistributed(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        event.setTotalCreditsDistributed(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        event.setStatus(ProfitDistributionStatus.PROCESSING);
        return profitDistributionRepository.save(event);
    }

    // Gói việc lưu lỗi chi tiết vào Transaction
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

    private Map<String, Vehicle> buildCompanyVehicleMap(Long companyId) {
        // buoc 1: tai toan bo xe thuoc company kem thong tin chu xe bang entity graph
        List<Vehicle> vehicles = vehicleRepository.findByCompanyId(companyId);
        Map<String, Vehicle> vehicleMap = new HashMap<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getEvOwner() == null) {
                continue;
            }
            String normalizedPlate = normalizePlate(vehicle.getPlateNumber());
            if (normalizedPlate != null && !normalizedPlate.isEmpty()) {
                vehicleMap.put(normalizedPlate, vehicle);
            }
        }
        return vehicleMap;
    }


    // Gói việc update Report vào Transaction
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

    /**
     * TÍNH TOÁN PAYOUT CHUẨN (LOGIC CỐT LÕI)
     * Tính toán số tiền payout (chi phí) dựa trên chính sách cố định.
     */
    private BigDecimal calculateRawPayout(ContributionData contribution, ResolvedPolicy policy) {
        // Logic 1: Nếu chính sách trả theo KWH
        if (policy.getPricingMode() == PricingMode.KWH && policy.getUnitPricePerKwh() != null) {
            return contribution.getTotalEnergyContribution()
                    .multiply(policy.getUnitPricePerKwh())
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // Logic 2: Nếu chính sách trả theo CREDIT
        return contribution.getTotalCreditContribution()
                .multiply(policy.getUnitPricePerCredit())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String normalizePlate(String plate) {
        return plate == null ? null : plate.replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * KIỂM TRA VÍ CÔNG TY
     * Đảm bảo công ty có đủ tiền để trả tổng chi phí (finalTotal).
     */
    private void validateCompanyBalance(Wallet companyWallet, BigDecimal requiredAmount) throws WalletException {
        if (requiredAmount == null) return;

        Wallet freshWallet = walletRepository.findById(companyWallet.getId())
                .orElseThrow(() -> new WalletException("Wallet not found during validation"));

        // SO SÁNH SỐ DƯ VÍ VỚI TỔNG TIỀN PHẢI TRẢ
        if (freshWallet.getBalance().compareTo(requiredAmount) < 0) {
            log.warn("Insufficient funds: Wallet {} has {} but requires {}",
                    freshWallet.getId(), freshWallet.getBalance(), requiredAmount);
            // NÉM LỖI CHUẨN
            throw new AppException(ErrorCode.WALLET_INSUFFICIENT_FUNDS);
        }
    }

    private BigDecimal resolveCreditContribution(EmissionReportDetail detail, BigDecimal fallbackEnergy) {
        if (detail == null) return BigDecimal.ZERO;
        BigDecimal co2Kg = detail.getCo2Kg();
        if (co2Kg != null && co2Kg.compareTo(BigDecimal.ZERO) > 0) {
            return co2Kg.setScale( 6, RoundingMode.HALF_UP);
        }
        if (fallbackEnergy != null && fallbackEnergy.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackEnergy.multiply(DEFAULT_EMISSION_FACTOR).setScale( 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(6,RoundingMode.HALF_UP);
    }
}

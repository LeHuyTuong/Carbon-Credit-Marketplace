package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus; // THAY ĐỔI
import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ProfitSharingServiceImpl implements ProfitSharingService {

    private static final BigDecimal KG_PER_CREDIT = new BigDecimal("1000");

    private final EmissionReportRepository emissionReportRepository;

    private final VehicleRepository vehicleRepository;
    private final EVOwnerRepository evOwnerRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final ProfitDistributionDetailRepository profitDistributionDetailRepository;
    private final ProjectRepository projectRepository;

    // // Inject TaskExecutor đã tạo ở Config
    // @Qualifier("profitSharingTaskExecutor")
    // private final TaskExecutor taskExecutor;


    // (Constructor của bạn)
    public ProfitSharingServiceImpl(
            EmissionReportRepository emissionReportRepository,
            VehicleRepository vehicleRepository,
            EVOwnerRepository evOwnerRepository,
            UserRepository userRepository,
            WalletService walletService,
            WalletRepository walletRepository,
            ProfitDistributionRepository profitDistributionRepository,
            ProfitDistributionDetailRepository profitDistributionDetailRepository,
            ProjectRepository projectRepository,
            @Qualifier("profitSharingTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.userRepository = userRepository;
        this.emissionReportRepository = emissionReportRepository;
        this.vehicleRepository = vehicleRepository;
        this.evOwnerRepository = evOwnerRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.profitDistributionRepository = profitDistributionRepository;
        this.profitDistributionDetailRepository = profitDistributionDetailRepository;
        this.projectRepository = projectRepository;
        // this.taskExecutor = taskExecutor; // (Không cần dùng nữa)
    }


    @Data
    private static class ContributionData {
        private Long evOwnerId;
        // tính theo Năng lượng
        private BigDecimal totalEnergyContribution = BigDecimal.ZERO;
        private BigDecimal totalCreditContribution = BigDecimal.ZERO;
        private List<EmissionReportDetail> reportDetails = new ArrayList<>();

        public ContributionData(Long evOwnerId) {
            this.evOwnerId = evOwnerId;
        }

        public void addContribution(BigDecimal energy, BigDecimal credit, EmissionReportDetail detail) {
            if (energy != null && energy.compareTo(BigDecimal.ZERO) > 0) {
                this.totalEnergyContribution = this.totalEnergyContribution.add(energy);
            }

            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                this.totalCreditContribution = this.totalCreditContribution.add(credit);
            }

            if (detail != null) {
                this.reportDetails.add(detail);
            }
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
        private final BigDecimal payoutAmount;
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

        log.info("Processing to share profit by company : {}", companyUser.getEmail());
        // 1. Tạo và lưu sự kiện chia lợi nhuận (Tự commit)
        ProfitDistribution distributionEvent = createDistributionEvent(request, companyUser);

        try {
            // 2. Lấy ví của công ty (Không nằm trong TX)
            Wallet companyWallet = walletService.findWalletByUser(companyUser);

            // 3. Lấy danh sách EmissionReport
            List<EmissionReport> reportsToProcess = new ArrayList<>();

            // Công ty muốn chia cho 1 report cụ thể
            if (request.getEmissionReportId() != null) {

                // ================== SỬA LỖI LAZY INIT ==================
                // Dùng hàm mới `findByIdWithDetails` để load cả 'details'
                EmissionReport report = emissionReportRepository.findByIdWithDetails(request.getEmissionReportId())
                        .orElseThrow(() -> new BadRequestException("No find emission report with id : " + request.getEmissionReportId()));
                // =======================================================

                if (report.getStatus() != EmissionStatus.CREDIT_ISSUED ) {
                    throw new AppException(ErrorCode.EMISSION_REPORT_NOT_APPROVED);
                }
                reportsToProcess.add(report);
            }

            if (reportsToProcess.isEmpty()) {
                log.warn("Do not have emission report is approved to share profit.");
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED); // Hoàn thành (không có gì để làm)
                distributionEvent.setDescription("Do not have report approve to share profit.");
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            // 5. (Bước 1 - Check) Lấy HashSet các biển số xe đã đăng ký
            log.info("Getting list of plateNumber is register with system.");
            Set<String> registeredPlates = vehicleRepository.findAllRegisteredPlateNumbers();
            log.info("Find {} PlateNumber is register ", registeredPlates.size());

            // 6. (Bước 1 & 2) Phân loại và Tổng hợp đóng góp TỪ TẤT CẢ CÁC REPORT
            Map<Long, ContributionData> evOwnerContributions = new HashMap<>();
            BigDecimal totalEnergyFromRegistered = BigDecimal.ZERO;
            BigDecimal totalEnergyFromUnregistered = BigDecimal.ZERO;
            BigDecimal totalCreditsFromRegistered = BigDecimal.ZERO;
            BigDecimal totalCreditsFromUnregistered = BigDecimal.ZERO;
            BigDecimal totalCreditsAll = BigDecimal.ZERO; // Tổng tín chỉ

            for (EmissionReport report : reportsToProcess) {
                // Vòng lặp này (trước đây là line 174) sẽ không còn lỗi "no Session"
                for (EmissionReportDetail detail : report.getDetails()) {

                    BigDecimal energy = detail.getTotalEnergy();
                    if (energy == null || energy.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal creditContribution = resolveCreditContribution(detail, energy);
                    totalCreditsAll = totalCreditsAll.add(creditContribution); // TÍNH TỔNG

                    if (registeredPlates.contains(detail.getVehiclePlate())) {
                        Vehicle vehicle = vehicleRepository.findByPlateNumberWithDetails(detail.getVehiclePlate()).orElse(null);
                        if (vehicle != null && vehicle.getEvOwner() != null) {
                            Long evOwnerId = vehicle.getEvOwner().getId();

                            ContributionData contribution = evOwnerContributions.computeIfAbsent(evOwnerId, ContributionData::new);
                            contribution.addContribution(energy, creditContribution, detail);

                            totalEnergyFromRegistered = totalEnergyFromRegistered.add(energy);
                            totalCreditsFromRegistered = totalCreditsFromRegistered.add(creditContribution);
                        }
                    } else {
                        totalEnergyFromUnregistered = totalEnergyFromUnregistered.add(energy);
                        totalCreditsFromUnregistered = totalCreditsFromUnregistered.add(creditContribution);
                    }
                }
            }


            log.info("Total energy contribution (Registered): {} kWh. (Unregistered): {} kWh.", totalEnergyFromRegistered, totalEnergyFromUnregistered);
            log.info("Total credit contribution (Registered): {}. (Unregistered): {}.", totalCreditsFromRegistered, totalCreditsFromUnregistered);

            BigDecimal sharePercent = request.getCompanySharePercent().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            BigDecimal requestedProfitBase = request.getTotalMoneyToDistribute();
            BigDecimal poolToShare = requestedProfitBase.multiply(sharePercent).setScale(2, RoundingMode.HALF_UP);

            if (totalCreditsAll.compareTo(BigDecimal.ZERO) <= 0 || totalCreditsFromRegistered.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("No eligible credit contributions from registered vehicles. Requested pool will remain in company wallet.");
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
                distributionEvent.setDescription(request.getDescription() + " | No registered vehicles eligible for payout");
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            // 7.1 Tạo các thanh toán
            List<OwnerPayoutData> payoutPlan = new ArrayList<>();
            BigDecimal totalMoneyToPayout = BigDecimal.ZERO;

            for (ContributionData contribution : evOwnerContributions.values()) {
                if (contribution.getTotalCreditContribution().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal ownerSharePercent = contribution.getTotalCreditContribution().divide(totalCreditsAll, 10, RoundingMode.HALF_UP);
                BigDecimal moneyToPay = poolToShare.multiply(ownerSharePercent).setScale(2, RoundingMode.HALF_UP);

                if (moneyToPay.compareTo(BigDecimal.ZERO) > 0) {
                    payoutPlan.add(new OwnerPayoutData(
                            contribution.getEvOwnerId(),
                            contribution.getTotalCreditContribution(),
                            contribution.getTotalEnergyContribution(),
                            moneyToPay
                    ));
                    totalMoneyToPayout = totalMoneyToPayout.add(moneyToPay);
                }
            } // HẾT VÒNG LẶP TÍNH TOÁN

            // 7.2 KIỂM TRA TỔNG TIỀN (1 LẦN)
            if (totalMoneyToPayout.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Computed payout amount is zero. No funds will be moved.");
                distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
                distributionEvent.setDescription(request.getDescription() + " | Payout amount resolved to zero");
                profitDistributionRepository.save(distributionEvent);
                return;
            }

            // KIỂM TRA VÍ 1 LẦN (An toàn, không còn deadlock)
            validateCompanyBalance(companyWallet, totalMoneyToPayout);

            // 7.3 Xác nhận
            BigDecimal undistributedAmount = poolToShare.subtract(totalMoneyToPayout).setScale(2, RoundingMode.HALF_UP);
            if (undistributedAmount.compareTo(BigDecimal.ZERO) < 0) {
                undistributedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            distributionEvent.setTotalMoneyDistributed(totalMoneyToPayout);
            distributionEvent.setTotalCreditsDistributed(totalCreditsFromRegistered.setScale(6, RoundingMode.HALF_UP));

            StringBuilder descriptionBuilder = new StringBuilder(request.getDescription());
            descriptionBuilder.append(" | Distributed ")
                    .append(totalMoneyToPayout.toPlainString())
                    .append(" from requested pool ")
                    .append(poolToShare.toPlainString());
            if (undistributedAmount.compareTo(BigDecimal.ZERO) > 0) {
                descriptionBuilder.append(" | Undistributed: ")
                        .append(undistributedAmount.toPlainString())
                        .append(" due to vehicles not registered in the system");
            }
            distributionEvent.setDescription(descriptionBuilder.toString());
            profitDistributionRepository.save(distributionEvent);

            // 7.4 Dùng VÒNG LẶP FOR TUẦN TỰ (Bỏ CompletableFuture)
            log.info("Starting sequential payout for {} owners...", payoutPlan.size());

            for (OwnerPayoutData payout : payoutPlan) {
                // Gọi hàm tuần tự (nó là public và @Transactional(REQUIRES_NEW))
                processPayoutForOwner(distributionEvent, payout, companyWallet);
            }

            log.info("All sequential payouts processed.");

            // 9. Cập nhật trạng thái EmissionReport -> PAID_OUT (Gói vào TX mới)
            updateReportsToPaidOut(reportsToProcess);

            // 10. Đánh dấu sự kiện là hoàn thành
            distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
            profitDistributionRepository.save(distributionEvent);

        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage(), e);
            String errorMessage = (e instanceof AppException ae) ? ae.getErrorCode().getMessage() : e.getMessage();

            distributionEvent.setStatus(ProfitDistributionStatus.FAILED);
            // Cắt ngắn lỗi (Sửa lỗi Data Truncation)
            if (errorMessage != null && errorMessage.length() > 250) {
                distributionEvent.setDescription("Error: " + errorMessage.substring(0, 250) + "...");
            } else {
                distributionEvent.setDescription("Error: " + errorMessage);
            }
            profitDistributionRepository.save(distributionEvent);
        }
    }


    /**
     * BƯỚC 3: Xử lý thanh toán (Hàm này tạo Lịch sử Giao dịch)
     * Hàm này chạy trong 1 GIAO DỊCH MỚI (REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processPayoutForOwner(ProfitDistribution event, OwnerPayoutData payout, Wallet companyWallet) {
        // Tải lại companyWallet (BẮT BUỘC)
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
            ownerWallet = ((WalletServiceImpl) walletService).generateWallet(owner.getUser());
            log.info("Wallet for EVOwner ID {} not found. Generated new wallet.", payout.getEvOwnerId());
        }

        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setDistribution(event);
        detail.setEvOwner(owner);
        detail.setMoneyAmount(payout.getPayoutAmount());
        detail.setCreditAmount(payout.getCreditContribution());

        try {
            if (payout.getPayoutAmount().compareTo(BigDecimal.ZERO) > 0) {
                walletService.transferFunds(
                        threadSafeCompanyWallet,
                        ownerWallet,
                        payout.getPayoutAmount(),
                        WalletTransactionType.PROFIT_SHARING.name(),
                        String.format("Sharing profit to EV owner %s (distribution #%d)", owner.getName(), event.getId()),
                        String.format("Profit-sharing from distribution #%d", event.getId())
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

    // Gói việc tạo Event vào Transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        event.setDescription(request.getDescription());
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
            detail.setStatus("FAILED");
            detail.setErrorMessage(errorMessage.substring(0, Math.min(errorMessage.length(), 254)));
            profitDistributionDetailRepository.save(detail);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to save failure detail for owner {}: {}", payout.getEvOwnerId(), e.getMessage());
        }
    }

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
        BigDecimal co2Kg = detail.getCo2Kg();
        if (co2Kg != null && co2Kg.compareTo(BigDecimal.ZERO) > 0) {
            return co2Kg.divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
        }
        if (fallbackEnergy != null && fallbackEnergy.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal defaultEF = new BigDecimal("0.4");
            return fallbackEnergy.multiply(defaultEF).divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}


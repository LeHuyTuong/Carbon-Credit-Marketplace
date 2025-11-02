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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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

    private final EmissionReportRepository emissionReportRepository;

    private final VehicleRepository vehicleRepository;
    private final EVOwnerRepository evOwnerRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final ProfitDistributionDetailRepository profitDistributionDetailRepository;
    private final ProjectRepository projectRepository;

    // Inject TaskExecutor đã tạo ở Config
    @Qualifier("profitSharingTaskExecutor")
    private final TaskExecutor taskExecutor;

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
            // BÁO CHO SPRING BIẾT: Tiêm chính xác bean tên là "profitSharingTaskExecutor" vào đây
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
        this.taskExecutor = taskExecutor; // Gán bean đã được chỉ định
    }

    /**
     * DTO nội bộ để tổng hợp đóng góp của mỗi chủ xe.
     */
    @Data
    private static class ContributionData {
        private Long evOwnerId;
        // SỬA LỖI LOGIC: Đổi tên biến để khớp với logic (tính theo Năng lượng)
        private BigDecimal totalEnergyContribution = BigDecimal.ZERO;
        private List<EmissionReportDetail> reportDetails = new ArrayList<>();

        public ContributionData(Long evOwnerId) {
            this.evOwnerId = evOwnerId;
        }

        public void addContribution(EmissionReportDetail detail) {
            // Logic tính toán dựa trên totalEnergy
            if (detail.getTotalEnergy() != null) {
                this.totalEnergyContribution = this.totalEnergyContribution.add(detail.getTotalEnergy());
                this.reportDetails.add(detail);
            }
        }
    }


    @Async("profitSharingTaskExecutor") // Chạy bất đồng bộ trên luồng riêng
    @Transactional(rollbackFor = Exception.class) // Bao bọc toàn bộ quá trình trong 1 transaction
    @Override
    public void shareCompanyProfit(ProfitSharingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User companyUser = userRepository.findByEmail(email);

        log.info("Processing to share profit by company : {}", companyUser.getEmail());
        // 1. Tạo và lưu sự kiện chia lợi nhuận
        ProfitDistribution distributionEvent = createDistributionEvent(request, companyUser);

        try {
            // 2. Lấy ví của công ty
            Wallet companyWallet = walletService.findWalletByUser(companyUser);

            // 3. Kiểm tra số dư của công ty
            validateCompanyBalance(companyWallet, request);

            // 4. Lấy danh sách EmissionReport
            List<EmissionReport> reportsToProcess = new ArrayList<>();

            // Trường hợp 1: Công ty muốn chia cho 1 report cụ thể
            if (request.getEmissionReportId() != null) {
                EmissionReport report = emissionReportRepository.findById(request.getEmissionReportId())
                        .orElseThrow(() -> new BadRequestException("No find emission report with id : " + request.getEmissionReportId()));
                if (report.getStatus() != EmissionStatus.APPROVED) {
                    throw new AppException(ErrorCode.EMISSION_REPORT_NOT_APPROVED); // CHUYỂN SANG AppException
                }
                reportsToProcess.add(report);
            }
            // Trường hợp 2: Công ty muốn chia cho all  report đã duyệt
            else {
                reportsToProcess = emissionReportRepository.findByStatus(EmissionStatus.APPROVED);
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

            for (EmissionReport report : reportsToProcess) {
                for (EmissionReportDetail detail : report.getDetails()) {

                    // Tính toán dựa trên Energy
                    BigDecimal energy = detail.getTotalEnergy();
                    if (energy == null || energy.compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // Bỏ qua nếu không có đóng góp
                    }

                    // (Bước 1) Nếu xe đã đăng ký
                    if (registeredPlates.contains(detail.getVehiclePlate())) {
                        Vehicle vehicle = vehicleRepository.findByPlateNumberWithDetails(detail.getVehiclePlate()).orElse(null);
                        if (vehicle != null && vehicle.getEvOwner() != null) {
                            Long evOwnerId = vehicle.getEvOwner().getId();

                            // Tổng hợp theo EV Owner
                            ContributionData contribution = evOwnerContributions.computeIfAbsent(evOwnerId, ContributionData::new);
                            contribution.addContribution(detail); // Thêm đóng góp từ detail

                            // SỬA LỖI LOGIC: Cộng dồn Energy
                            totalEnergyFromRegistered = totalEnergyFromRegistered.add(energy);
                        }
                    }
                    // (Bước 2) Xe không đăng ký
                    else {
                        totalEnergyFromUnregistered = totalEnergyFromUnregistered.add(energy);
                    }
                }
            }


            log.info("Total energy contribution (Registered): {} kWh. (Unregistered): {} kWh.", totalEnergyFromRegistered, totalEnergyFromUnregistered);

            // 7. (Bước 4 - Concurrency) Tạo các tác vụ thanh toán
            if (totalEnergyFromRegistered.compareTo(BigDecimal.ZERO) > 0) {
                List<CompletableFuture<Void>> payoutTasks = new ArrayList<>();

                for (Map.Entry<Long, ContributionData> entry : evOwnerContributions.entrySet()) {
                    ContributionData contribution = entry.getValue();

                    // Tính toán phần trăm đóng góp (SỬA LỖI LOGIC)
                    BigDecimal ownerSharePercent = contribution.getTotalEnergyContribution()
                            .divide(totalEnergyFromRegistered, 10, RoundingMode.HALF_UP);

                    // Tính số tiền được nhận
                    BigDecimal moneyToPay = request.getTotalMoneyToDistribute().multiply(ownerSharePercent).setScale(2, RoundingMode.HALF_UP);

                    // Tạo một tác vụ chạy bất đồng bộ
                    CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                        processPayoutForOwner(distributionEvent, contribution.getEvOwnerId(), moneyToPay, companyWallet);
                    }, taskExecutor); // Sử dụng taskExecutor đã định nghĩa

                    payoutTasks.add(task);
                }

                // 8. Chờ tất cả các tác vụ thanh toán hoàn thành
                log.info("Waiting for {} payout tasks to complete...", payoutTasks.size());
                CompletableFuture.allOf(payoutTasks.toArray(new CompletableFuture[0])).join(); // join() để chờ
                log.info("All payout tasks completed.");

            } else {
                // Trường hợp "không có gì để làm" (không có xe đăng ký nào đóng góp)
                log.warn("No energy contributions from registered vehicles. All profit remains with the company.");
            }

            // 9. Cập nhật trạng thái EmissionReport -> PAID_OUT
            for (EmissionReport report : reportsToProcess) {
                report.setStatus(EmissionStatus.PAID_OUT);
            }
            emissionReportRepository.saveAll(reportsToProcess);

            // 10. Đánh dấu sự kiện là hoàn thành
            distributionEvent.setStatus(ProfitDistributionStatus.COMPLETED);
            profitDistributionRepository.save(distributionEvent);

        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage(), e);
            // Lấy message lỗi từ ErrorCode nếu có
            String errorMessage = (e instanceof AppException ae) ? ae.getErrorCode().getMessage() : e.getMessage();

            distributionEvent.setStatus(ProfitDistributionStatus.FAILED);
            distributionEvent.setDescription("Error: " + errorMessage);
            profitDistributionRepository.save(distributionEvent);
            // Vì @Transactional, tất cả các report status sẽ được rollback,
            // không có report nào bị set thành PAID_OUT -> an toàn để chạy lại.
        }
    }

    /**
     * Xử lý thanh toán cho một EV Owner cụ thể.
     * Được gọi bên trong một luồng riêng (async).
     */
    private void processPayoutForOwner(ProfitDistribution event, Long evOwnerId, BigDecimal money, Wallet companyWallet) {
        // Tải lại companyWallet trong luồng này để đảm bảo an toàn
        Wallet threadSafeCompanyWallet = walletRepository.findById(companyWallet.getId()).orElse(null);
        if (threadSafeCompanyWallet == null) {
            log.error("Company wallet not found in async thread! Wallet ID: {}", companyWallet.getId());
            return;
        }

        EVOwner owner = evOwnerRepository.findById(evOwnerId).orElse(null);
        if (owner == null || owner.getUser() == null) {
            log.error("EVOwner or associated User not found for EVOwner ID: {}", evOwnerId);
            return;
        }

        Wallet ownerWallet = walletService.findWalletByUser(owner.getUser());
        ProfitDistributionDetail detail = new ProfitDistributionDetail();
        detail.setDistribution(event);
        detail.setEvOwner(owner);
        detail.setMoneyAmount(money);

        try {
            // Thực hiện chuyển tiền (nếu có)
            if (money.compareTo(BigDecimal.ZERO) > 0) {
                walletService.transferFunds(
                        threadSafeCompanyWallet,
                        ownerWallet,
                        money,
                        WalletTransactionType.PROFIT_SHARING.name(),
                        "Profit sharing payout for Batch #" + event.getId()
                );
            }

            detail.setStatus("SUCCESS");
            log.info("Successfully processed payout for EVOwner ID: {}", evOwnerId);

        } catch (WalletException e) {
            log.error("WalletException during payout for EVOwner ID {}: {}", evOwnerId, e.getMessage());
            detail.setStatus("FAILED");
            detail.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("System error during payout for EVOwner ID {}: {}", evOwnerId, e.getMessage(), e);
            detail.setStatus("FAILED");
            detail.setErrorMessage("System error: " + e.getMessage());
        } finally {
            profitDistributionDetailRepository.save(detail); // Lưu lại chi tiết dù thành công hay thất bại
        }
    }

    // --- Helper Methods ---
    private ProfitDistribution createDistributionEvent(ProfitSharingRequest request, User companyUser) {
        ProfitDistribution event = new ProfitDistribution();
        event.setCompanyUser(companyUser);
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy Project với ID: " + request.getProjectId()));
            event.setProject(project);
        }
        event.setTotalMoneyDistributed(request.getTotalMoneyToDistribute());
        event.setDescription(request.getDescription());
        event.setStatus(ProfitDistributionStatus.PROCESSING); // Bắt đầu xử lý ngay
        return profitDistributionRepository.save(event);
    }

    private void validateCompanyBalance(Wallet companyWallet, ProfitSharingRequest request) throws WalletException {
        if (companyWallet.getBalance().compareTo(request.getTotalMoneyToDistribute()) < 0) {
            throw new AppException(ErrorCode.WALLET_INSUFFICIENT_FUNDS);
        }
    }
}


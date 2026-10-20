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
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.*;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service chịu trách nhiệm phân chia lợi nhuận (Payout) cho các chủ xe điện (EV Owners).
 * Logic bao gồm: Tính toán đóng góp, trừ tiền ví công ty, cộng tiền ví Owner, và gửi email thông báo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProfitSharingServiceImpl implements ProfitSharingService {

    private static final BigDecimal DEFAULT_EMISSION_FACTOR = new BigDecimal("0.6");
    private static final String DETAIL_PENDING = "PENDING";
    private static final String DETAIL_SUCCESS = "SUCCESS";
    private static final String DETAIL_FAILED = "FAILED";
    private static final String DETAIL_SKIPPED = "SKIPPED";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
    private final PlatformTransactionManager transactionManager;
    private final ProfitSharingTaskDispatcher retryDispatcher;

    // TransactionTemplate REQUIRES_NEW dùng chung, khởi tạo sau khi inject transactionManager.
    // Thay cho self-proxy trước đây (gọi @Transactional qua chính bean).
    private TransactionTemplate requiresNewTransaction;

    @PostConstruct
    void initTransactionTemplate() {
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

        OwnerPayoutData(Long evOwnerId,
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
     *
     * <p>{@code companyUserId} được controller xác thực và truyền vào, vì async thread
     * không có SecurityContext.</p>
     */
    @Async("profitSharingTaskExecutor")
    @Override
    public void shareCompanyProfit(Long companyUserId, ProfitSharingRequest request) {
        // B1: Load lại company user theo ID (không đọc SecurityContext trong async thread)
        User companyUser = userRepository.findById(companyUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

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

        // Lease token của riêng lần chạy này. Mọi heartbeat/hoàn tất phải khớp token,
        // nên một lần chạy bị reclaim không thể ghi đè trạng thái của claim mới.
        String claimToken = UUID.randomUUID().toString();
        ProfitDistribution distributionEvent = null;

        try {
            Wallet companyWallet = walletService.findWalletByUser(companyUser);
            List<EmissionReport> reportsToProcess = new ArrayList<>();

            // B4: Validate và Load Emission Report
            EmissionReport report;

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
                return;
            }

            if (reportsToProcess.isEmpty()) {
                return;
            }

            // B3: Tạo bản ghi theo dõi đợt phân phối (Status: PROCESSING).
            // Guard + insert nằm trong CÙNG transaction có khóa row report, nên hai request
            // /share đồng thời không thể cùng thấy "chưa có payout" rồi cùng trả tiền.
            distributionEvent = createDistributionEvent(request, companyUser, company, report.getId(), claimToken);

            // B5: Chuẩn bị Map mapping từ Biển số xe -> Vehicle Entity để tra cứu nhanh
            Map<String, Vehicle> vehicleByPlate = buildCompanyVehicleMap(company.getId());
            if (vehicleByPlate.isEmpty()) {
                completeUnfundedDistribution(distributionEvent, claimToken);
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
                completeUnfundedDistribution(distributionEvent, claimToken);
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
                completeUnfundedDistribution(distributionEvent, claimToken);
                return;
            }

            BigDecimal finalTotal = totalRawPayout.setScale(2, RoundingMode.HALF_UP);

            // B8: Kiểm tra số dư ví công ty trước khi thực hiện
            validateCompanyBalance(companyWallet, finalTotal);

            // Update thông tin tổng vào sự kiện phân phối
            distributionEvent.setTotalMoneyDistributed(finalTotal);
            distributionEvent.setTotalCreditsDistributed(totalCreditsKgForDistribution.setScale(6, RoundingMode.HALF_UP));
            profitDistributionRepository.save(distributionEvent);

            // B9: Ghi TRƯỚC toàn bộ kế hoạch chi trả thành detail PENDING.
            // Nếu tiến trình chết giữa chừng, các owner chưa trả vẫn còn một row PENDING
            // để retry; không phụ thuộc vào việc ghi FAILED detail có thành công hay không.
            List<ProfitDistributionDetail> plannedDetails =
                    persistPayoutPlan(distributionEvent, payoutPlan);

            log.info("Starting sequential payout for {} owners...", payoutPlan.size());

            // B10: Thực hiện chuyển tiền tuần tự cho từng Owner
            // Mỗi owner chạy trong transaction REQUIRES_NEW riêng để lỗi 1 owner không rollback owner khác.
            // Heartbeat trước mỗi owner: một /share dài không bị /retry coi là stale.
            for (int i = 0; i < payoutPlan.size(); i++) {
                if (!touchDistribution(distributionEvent.getId(), claimToken)) {
                    log.warn("Distribution #{} is no longer owned by claim {}; stopping /share before owner {}",
                            distributionEvent.getId(), claimToken, payoutPlan.get(i).getEvOwnerId());
                    return;
                }
                processPayoutForOwner(
                        distributionEvent,
                        payoutPlan.get(i),
                        companyWallet,
                        report.getId(), // Chỉ truyền ID để load lại trong transaction con
                        policy,
                        company,
                        plannedDetails.get(i).getId());
            }

            // B11: Chỉ hoàn tất khi ĐỌC LẠI thấy mọi detail đều SUCCESS/SKIPPED.
            // Nếu một detail còn PENDING (ví dụ ghi FAILED thất bại), distribution phải
            // FAILED để còn đường retry, tuyệt đối không đánh PAID_OUT.
            List<ProfitDistributionDetail> persistedDetails =
                    profitDistributionDetailRepository.findByDistributionIdWithOwner(distributionEvent.getId());
            if (!allSettled(persistedDetails)) {
                log.error("Distribution #{} still has unsettled owner payouts; report #{} stays CREDIT_ISSUED for retry",
                        distributionEvent.getId(), report.getId());
                failDistribution(distributionEvent.getId(), claimToken);
                return;
            }

            log.info("All sequential payouts processed.");

            // B11: Hoàn tất distribution + report trong CÙNG transaction, chỉ khi claim token
            // còn là của mình. Nếu một worker khác đã claim lại, không được đánh COMPLETED.
            if (!settleDistribution(distributionEvent, persistedDetails, claimToken)) {
                return;
            }

            // B12: Gửi email tổng kết cho Công ty (sau khi trạng thái đã commit)
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

        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage(), e);
            if (distributionEvent != null) {
                failDistribution(distributionEvent.getId(), claimToken);
            }
        }
    }

    /**
     * Validate + claim quyền retry trên request thread, rồi mới đẩy phần trả tiền sang
     * pool profit-sharing. Nhờ vậy lỗi 400/403/404 được trả ngay cho client.
     */
    @Override
    public void requestRetry(Long companyUserId, Long distributionId) {
        User companyUser = userRepository.findById(companyUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        companyRepository.findByUserId(companyUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        ProfitDistribution distribution = profitDistributionRepository.findWithReportById(distributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Profit distribution not found: " + distributionId));

        if (!Objects.equals(distribution.getCompanyUser().getId(), companyUser.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        rejectSupersededDistribution(distribution, companyUserId, distributionId);

        ProfitDistributionStatus currentStatus = distribution.getStatus();
        // Legacy: base cũ đánh COMPLETED dù còn owner FAILED. Chỉ cho retry khi thực sự
        // còn detail chưa settled; distribution đã trả đủ vẫn bị từ chối như trước.
        boolean completedWithUnpaidOwners = currentStatus == ProfitDistributionStatus.COMPLETED
                && profitDistributionDetailRepository.existsUnsettledByDistributionId(distributionId);
        if (currentStatus == ProfitDistributionStatus.COMPLETED && !completedWithUnpaidOwners) {
            throw new BadRequestException("Distribution #" + distributionId + " is already completed.");
        }

        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        long staleMinutes = Math.max(1, profitSharingProperties.getStaleProcessingMinutes());
        OffsetDateTime staleBefore = now.minusMinutes(staleMinutes);
        String claimToken = UUID.randomUUID().toString();

        // Compare-and-set trong transaction riêng: hai request retry đồng thời chỉ một request thắng.
        // markRetrying/claimStaleProcessing đều ghi claimToken + updatedAt trong cùng câu UPDATE,
        // nên request thứ hai đọc lại status PROCESSING sẽ thấy heartbeat mới và bị từ chối.
        Integer claimed = requiresNewTransaction.execute(status -> {
            if (currentStatus == ProfitDistributionStatus.PROCESSING) {
                // Crash/deploy để lại PROCESSING: chỉ nhận lại khi không có heartbeat
                // trong staleMinutes, tránh giẫm lên một job đang thực sự chạy.
                return profitDistributionRepository.claimStaleProcessing(
                        distributionId, ProfitDistributionStatus.PROCESSING, staleBefore, now, claimToken);
            }
            if (currentStatus == ProfitDistributionStatus.FAILED
                    || currentStatus == ProfitDistributionStatus.PENDING
                    || (currentStatus == ProfitDistributionStatus.COMPLETED && completedWithUnpaidOwners)) {
                return profitDistributionRepository.markRetrying(
                        distributionId, ProfitDistributionStatus.PROCESSING, currentStatus, claimToken, now);
            }
            return 0;
        });

        if (claimed == null || claimed == 0) {
            if (currentStatus == ProfitDistributionStatus.PROCESSING) {
                throw new BadRequestException("Distribution #" + distributionId
                        + " is still running. Retry is allowed after " + staleMinutes
                        + " minutes without progress.");
            }
            throw new BadRequestException("Distribution #" + distributionId
                    + " is not retryable (status " + currentStatus + ").");
        }

        try {
            retryDispatcher.submit(() -> executeRetry(companyUserId, distributionId, claimToken));
        } catch (RuntimeException e) {
            log.error("Could not enqueue retry for distribution #{}: {}", distributionId, e.getMessage(), e);
            // Trả status về FAILED để không kẹt PROCESSING khi executor từ chối task.
            failDistribution(distributionId, claimToken);
            throw new AppException(ErrorCode.TRANSACTION_PROCESSING_ERROR);
        }
    }

    /**
     * Chặn retry một distribution đã bị đợt mới hơn thay thế. Đây là hàng rào chính
     * cho F2: D1 có thể vẫn FAILED, nhưng nếu D2 mới hơn đã trả một owner thì retry D1
     * sẽ trả lại owner đó lần nữa.
     */
    private void rejectSupersededDistribution(ProfitDistribution distribution,
                                              Long companyUserId,
                                              Long distributionId) {
        EmissionReport report = distribution.getEmissionReport();
        Optional<ProfitDistribution> latest;
        if (report != null) {
            latest = profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(report.getId());
        } else if (distribution.getProject() != null) {
            latest = profitDistributionRepository.findFirstByCompanyUserIdAndProjectIdOrderByIdDesc(
                    companyUserId, distribution.getProject().getId());
        } else {
            // Distribution quá cũ, không có cả report lẫn project: chỉ dám so theo công ty.
            latest = profitDistributionRepository.findFirstByCompanyUserIdOrderByIdDesc(companyUserId);
        }

        if (latest.isPresent() && !Objects.equals(latest.get().getId(), distributionId)) {
            throw new BadRequestException("Distribution #" + distributionId
                    + " was superseded by distribution #" + latest.get().getId()
                    + "; retry the latest distribution instead.");
        }
    }

    /**
     * Phần chạy nền của retry. Distribution đã được claim sang PROCESSING trước đó.
     */
    private void executeRetry(Long companyUserId, Long distributionId, String claimToken) {
        try {
            // Fence đầu tiên: nếu một worker khác đã claim lại (token đổi), dừng ngay
            // trước khi đọc/trả bất kỳ owner nào.
            if (!touchDistribution(distributionId, claimToken)) {
                log.warn("Retry for distribution #{} skipped: claim {} is no longer current",
                        distributionId, claimToken);
                return;
            }

            User companyUser = userRepository.findById(companyUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            Company company = companyRepository.findByUserId(companyUser.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
            ProfitDistribution distribution = profitDistributionRepository.findWithReportById(distributionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Profit distribution not found: " + distributionId));

            List<ProfitDistributionDetail> details = profitDistributionDetailRepository
                    .findByDistributionIdWithOwner(distributionId);
            List<ProfitDistributionDetail> unpaidDetails = details.stream()
                    .filter(this::isUnpaidDetail)
                    .toList();

            if (unpaidDetails.isEmpty()) {
                settleDistribution(distribution, details, claimToken);
                return;
            }

            EmissionReport report = distribution.getEmissionReport();
            // PAID_OUT được chấp nhận cho legacy: base cũ đánh report PAID_OUT dù còn
            // owner FAILED; retry chỉ trả các detail chưa settled nên không double-pay.
            if (report != null
                    && report.getStatus() != EmissionStatus.CREDIT_ISSUED
                    && report.getStatus() != EmissionStatus.PAID_OUT) {
                throw new BadRequestException("Emission report " + report.getId()
                        + " is not waiting for payout (status " + report.getStatus() + ").");
            }

            Wallet companyWallet = walletService.findWalletByUser(companyUser);
            if (companyWallet == null) {
                throw new BadRequestException("Company wallet not found.");
            }

            BigDecimal requiredAmount = unpaidDetails.stream()
                    .map(detail -> Objects.requireNonNullElse(detail.getMoneyAmount(), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            validateCompanyBalance(companyWallet, requiredAmount);

            ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
            Long reportId = report == null ? null : report.getId();
            for (ProfitDistributionDetail detail : unpaidDetails) {
                if (!touchDistribution(distributionId, claimToken)) {
                    log.warn("Retry for distribution #{} stopped before owner {}: claim {} is no longer current",
                            distributionId, detail.getEvOwner().getId(), claimToken);
                    return;
                }
                OwnerPayoutData payout = new OwnerPayoutData(
                        detail.getEvOwner().getId(),
                        detail.getCreditAmount(),
                        detail.getEnergyAmount(),
                        detail.getMoneyAmount());
                processPayoutForOwner(
                        distribution,
                        payout,
                        companyWallet,
                        reportId,
                        policy,
                        company,
                        detail.getId());
            }

            List<ProfitDistributionDetail> freshDetails = profitDistributionDetailRepository
                    .findByDistributionIdWithOwner(distributionId);
            settleDistribution(distribution, freshDetails, claimToken);
        } catch (Exception e) {
            log.error("Retry of distribution #{} failed: {}", distributionId, e.getMessage(), e);
            // Chỉ reset khi vẫn giữ claim; nếu worker khác đã claim lại thì không ghi đè.
            failDistribution(distributionId, claimToken);
        }
    }

    /**
     * Hoàn tất distribution khi mọi detail đã SUCCESS/SKIPPED; ngược lại đánh FAILED
     * để còn đường retry. Việc đổi trạng thái distribution và report nằm trong CÙNG
     * transaction, và chỉ thành công khi claim token vẫn là của worker gọi.
     *
     * @return true khi distribution thực sự được chuyển sang COMPLETED.
     */
    private boolean settleDistribution(ProfitDistribution distribution,
                                       List<ProfitDistributionDetail> details,
                                       String claimToken) {
        if (!allSettled(details)) {
            failDistribution(distribution.getId(), claimToken);
            return false;
        }

        EmissionReport report = distribution.getEmissionReport();
        if (report == null) {
            // Distribution cũ trước migration: vẫn trả được owner còn nợ, nhưng không có
            // căn cứ an toàn để tự đánh dấu report nào là PAID_OUT.
            log.warn("Legacy distribution #{} has no linked emission report; owner payouts are settled "
                    + "but the emission report status cannot be updated automatically.", distribution.getId());
        } else if (report.getStatus() != EmissionStatus.CREDIT_ISSUED
                && report.getStatus() != EmissionStatus.PAID_OUT) {
            throw new BadRequestException("Emission report " + report.getId()
                    + " is not waiting for payout (status " + report.getStatus() + ").");
        }

        Long reportId = report == null ? null : report.getId();
        Boolean completed = requiresNewTransaction.execute(status -> {
            int updated = profitDistributionRepository.finishIfClaimed(
                    distribution.getId(),
                    ProfitDistributionStatus.COMPLETED,
                    ProfitDistributionStatus.PROCESSING,
                    claimToken,
                    OffsetDateTime.now(BUSINESS_ZONE));
            if (updated == 0) {
                log.warn("Distribution #{} is no longer owned by claim {}; not marking COMPLETED",
                        distribution.getId(), claimToken);
                return false;
            }

            if (reportId != null) {
                EmissionReport freshReport = emissionReportRepository.findById(reportId).orElse(null);
                if (freshReport == null) {
                    // Rollback cả việc hoàn tất distribution: không được đánh COMPLETED
                    // khi report biến mất giữa chừng.
                    throw new BadRequestException("Emission report not found: " + reportId);
                }
                if (freshReport.getStatus() == EmissionStatus.CREDIT_ISSUED) {
                    freshReport.setStatus(EmissionStatus.PAID_OUT);
                    emissionReportRepository.save(freshReport);
                } else if (freshReport.getStatus() != EmissionStatus.PAID_OUT) {
                    throw new BadRequestException("Emission report " + reportId
                            + " is not waiting for payout (status " + freshReport.getStatus() + ").");
                }
            }

            distribution.setStatus(ProfitDistributionStatus.COMPLETED);
            return true;
        });

        return Boolean.TRUE.equals(completed);
    }

    /**
     * Chuyển distribution sang FAILED chỉ khi worker còn giữ claim. Không dùng
     * {@code save(entity)} vì entity có thể đã cũ và sẽ ghi đè claim mới.
     */
    private void failDistribution(Long distributionId, String claimToken) {
        if (distributionId == null) return;
        try {
            requiresNewTransaction.executeWithoutResult(status -> {
                int updated = profitDistributionRepository.finishIfClaimed(
                        distributionId,
                        ProfitDistributionStatus.FAILED,
                        ProfitDistributionStatus.PROCESSING,
                        claimToken,
                        OffsetDateTime.now(BUSINESS_ZONE));
                if (updated == 0) {
                    log.warn("Distribution #{} is no longer owned by claim {}; status left unchanged",
                            distributionId, claimToken);
                }
            });
        } catch (Exception e) {
            log.error("Could not mark distribution #{} FAILED: {}", distributionId, e.getMessage());
        }
    }

    /**
     * Dùng cho các nhánh không có owner nào để trả (không có xe/contribution/payout):
     * đánh COMPLETED nhưng không đánh report PAID_OUT vì chưa có ai nhận tiền.
     */
    private void completeUnfundedDistribution(ProfitDistribution distribution, String claimToken) {
        try {
            Boolean completed = requiresNewTransaction.execute(status ->
                    profitDistributionRepository.finishIfClaimed(
                            distribution.getId(),
                            ProfitDistributionStatus.COMPLETED,
                            ProfitDistributionStatus.PROCESSING,
                            claimToken,
                            OffsetDateTime.now(BUSINESS_ZONE)) > 0);
            if (Boolean.TRUE.equals(completed)) {
                distribution.setStatus(ProfitDistributionStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Could not complete distribution #{}: {}", distribution.getId(), e.getMessage());
        }
    }

    /**
     * Heartbeat + fence: trả false khi distribution không còn PROCESSING hoặc claim
     * token đã đổi chủ, nghĩa là worker này phải dừng, không được trả thêm owner nào.
     */
    private boolean touchDistribution(Long distributionId, String claimToken) {
        try {
            OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
            Integer updated = requiresNewTransaction.execute(status ->
                    profitDistributionRepository.touchProcessing(
                            distributionId, ProfitDistributionStatus.PROCESSING, now, claimToken));
            return updated != null && updated > 0;
        } catch (Exception e) {
            log.warn("Could not heartbeat distribution #{}: {}", distributionId, e.getMessage());
            return false;
        }
    }

    private boolean isUnpaidDetail(ProfitDistributionDetail detail) {
        return DETAIL_FAILED.equalsIgnoreCase(detail.getStatus())
                || DETAIL_PENDING.equalsIgnoreCase(detail.getStatus());
    }

    private boolean isSettledDetail(ProfitDistributionDetail detail) {
        return DETAIL_SUCCESS.equalsIgnoreCase(detail.getStatus())
                || DETAIL_SKIPPED.equalsIgnoreCase(detail.getStatus());
    }

    private boolean allSettled(List<ProfitDistributionDetail> details) {
        return details != null && !details.isEmpty() && details.stream().allMatch(this::isSettledDetail);
    }

    /**
     * Xử lý chuyển tiền cho MỘT Owner cụ thể.
     * IMPORTANT: dùng TransactionTemplate với Propagation.REQUIRES_NEW để tạo một transaction
     * hoàn toàn mới. Nếu transaction này fail (ví dụ lỗi mạng, lỗi ví), nó chỉ rollback cho
     * Owner này, KHÔNG ảnh hưởng đến các Owner khác đã được trả tiền.
     *
     * <p>Method là package-private thay vì private để test hành vi rollback-only gọi trực tiếp;
     * transaction được mở tường minh bằng {@link #requiresNewTransaction} thay vì self-proxy.
     * Đây không phải API công khai.</p>
     */
    boolean processPayoutForOwner(
            ProfitDistribution event,
            OwnerPayoutData payout,
            Wallet companyWallet,
            Long reportId,
            ResolvedPolicy policy,
            Company company,
            Long existingDetailId
    ) {
        try {
            String failure = requiresNewTransaction.execute(status ->
                    doProcessPayoutForOwner(event, payout, companyWallet, reportId, policy, company, existingDetailId));

            if (failure != null) {
                saveFailedDetail(event, payout, failure, existingDetailId);
                return false;
            }
            return true;
        } catch (UnexpectedRollbackException e) {
            // doProcessPayoutForOwner đã catch exception gốc nên transaction REQUIRES_NEW bị đánh dấu
            // rollback-only; commit sau đó ném UnexpectedRollbackException. Chỉ nuốt lỗi commit này
            // để vòng lặp đi tiếp owner kế tiếp thay vì đánh FAILED cả distribution.
            log.error("Payout for owner {} in distribution {} hit a rollback-only transaction: {}",
                    payout.getEvOwnerId(), event.getId(), e.getMessage(), e);
            // Ghi đúng MỘT detail FAILED: nếu đây là retry thì cập nhật detail cũ,
            // không tạo thêm dòng thứ hai cho cùng owner.
            saveFailedDetail(event, payout, String.format(
                    "Payout rolled back (distribution #%d, owner %d): %s",
                    event.getId(), payout.getEvOwnerId(), e.getMessage()), existingDetailId);
            return false;
        } catch (Exception e) {
            // Lỗi hạ tầng của riêng owner này (DB, kết nối...) không được làm dừng các owner khác.
            log.error("Payout for owner {} in distribution {} failed: {}",
                    payout.getEvOwnerId(), event.getId(), e.getMessage(), e);
            saveFailedDetail(event, payout, "Payout failed: " + errorMessage(e), existingDetailId);
            return false;
        }
    }

    /**
     * @return {@code null} khi owner đã được trả tiền và detail SUCCESS đã commit;
     *         ngược lại trả về lý do thất bại để tầng ngoài ghi một detail FAILED duy nhất.
     */
    private String doProcessPayoutForOwner(
            ProfitDistribution event,
            OwnerPayoutData payout,
            Wallet companyWallet,
            Long reportId,
            ResolvedPolicy policy,
            Company company,
            Long existingDetailId
    ) {
        // Khóa row detail NGAY ĐẦU transaction. Hai worker cùng xử lý một owner sẽ bị
        // tuần tự hóa: worker thứ hai chờ commit rồi thấy SUCCESS/SKIPPED và bỏ qua,
        // nên transferFunds cho cùng owner không thể chạy song song.
        ProfitDistributionDetail detail = null;
        if (existingDetailId != null) {
            detail = profitDistributionDetailRepository.findByIdForUpdate(existingDetailId).orElse(null);
            if (detail == null) {
                return "Distribution detail not found: " + existingDetailId;
            }
            if (detail.getDistribution() == null
                    || !Objects.equals(detail.getDistribution().getId(), event.getId())) {
                return "Distribution detail " + existingDetailId
                        + " does not belong to distribution #" + event.getId();
            }
            // Retry có thể chạy lại sau khi request trước đã trả xong owner này.
            // Tuyệt đối không chuyển tiền lần hai.
            if (isSettledDetail(detail)) {
                return null;
            }
        }

        // Load lại Report trong session mới để tránh LazyInitializationException
        EmissionReport report = emissionReportRepository.findByIdWithDetails(reportId).orElse(null);

        String projectName = "N/A";
        String reportPeriod = "N/A";
        String reportIdStr = reportId == null ? "N/A" : String.valueOf(reportId);

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
            return "Company wallet not found";
        }

        EVOwner owner = evOwnerRepository.findById(payout.getEvOwnerId()).orElse(null);
        if (owner == null || owner.getUser() == null) {
            return "EVOwner not found";
        }

        // Tìm hoặc Tự động tạo ví cho Owner nếu chưa có
        Wallet ownerWallet = walletService.findWalletByUser(owner.getUser());
        if (ownerWallet == null) {
            try {
                ownerWallet = ((WalletServiceImpl) walletService).generateWallet(owner.getUser());
                log.info("Generated new wallet for EVOwner ID {}", payout.getEvOwnerId());
            } catch (Exception e) {
                // Không save detail ở đây: transaction hiện tại đã rollback-only.
                // Tầng ngoài sẽ ghi đúng một detail FAILED sau khi commit thất bại.
                return "Failed to create wallet: " + errorMessage(e);
            }
        }

        if (detail == null) {
            detail = new ProfitDistributionDetail();
            detail.setDistribution(event);
            detail.setEvOwner(owner);
        }

        detail.setMoneyAmount(payout.getPayoutAmount());
        detail.setCreditAmount(payout.getCreditContribution());
        detail.setEnergyAmount(payout.getEnergyContribution());
        detail.setErrorMessage(null);

        try {
            // 0. Defense-in-depth: dữ liệu cũ có thể có hai distribution cho cùng report.
            // Nếu owner đã SUCCESS ở distribution khác thì đánh SKIPPED (đã trả ở nơi khác),
            // không chuyển tiền lần nữa và không chặn việc hoàn tất distribution này.
            if (reportId != null
                    && profitDistributionDetailRepository.existsPaidInAnotherDistribution(
                    reportId, payout.getEvOwnerId(), event.getId())) {
                detail.setStatus(DETAIL_SKIPPED);
                detail.setErrorMessage("Already paid in another distribution for report #" + reportId);
                profitDistributionDetailRepository.save(detail);
                log.warn("Skipping owner {} in distribution #{}: already paid by another distribution for report #{}",
                        payout.getEvOwnerId(), event.getId(), reportId);
                return null;
            }

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

            detail.setStatus(DETAIL_SUCCESS);
            profitDistributionDetailRepository.save(detail);
            return null;

        } catch (Exception e) {
            // Không save FAILED ở đây: nếu transferFunds đã đánh dấu rollback-only,
            // việc save trong cùng transaction sẽ bị rollback và tầng ngoài sẽ ghi lại.
            log.error("Exception during payout for EVOwner ID {}: {}", payout.getEvOwnerId(), e.getMessage());
            return errorMessage(e);
        }
    }

    /**
     * Tạo distribution + guard trong CÙNG transaction, sau khi khóa row report bằng
     * PESSIMISTIC_WRITE. Đây là điểm tuần tự hóa hai request /share đồng thời.
     */
    private ProfitDistribution createDistributionEvent(
            ProfitSharingRequest request,
            User companyUser,
            Company company,
            Long reportId,
            String claimToken
    ) {
        return requiresNewTransaction.execute(status -> {
            EmissionReport lockedReport = emissionReportRepository.findByIdForUpdate(reportId)
                    .orElseThrow(() -> new BadRequestException(
                            "No find emission report with id : " + reportId));

            if (lockedReport.getStatus() != EmissionStatus.CREDIT_ISSUED) {
                throw new AppException(ErrorCode.EMISSION_REPORT_NOT_APPROVED);
            }
            if (!Objects.equals(lockedReport.getSeller().getId(), company.getId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }

            rejectIfPayoutAlreadyStarted(lockedReport, companyUser.getId());

            ProfitDistribution event = new ProfitDistribution();
            event.setCompanyUser(companyUser);
            event.setEmissionReport(lockedReport);
            event.setProject(lockedReport.getProject());
            if (request.getProjectId() != null
                    && (lockedReport.getProject() == null
                    || !Objects.equals(request.getProjectId(), lockedReport.getProject().getId()))) {
                Project project = projectRepository.findById(request.getProjectId())
                        .orElseThrow(() -> new BadRequestException("Project ID not found: " + request.getProjectId()));
                event.setProject(project);
            }
            event.setTotalMoneyDistributed(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            event.setTotalCreditsDistributed(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            event.setStatus(ProfitDistributionStatus.PROCESSING);
            event.setClaimToken(claimToken);
            return profitDistributionRepository.save(event);
        });
    }

    /**
     * Ghi toàn bộ payout plan thành detail PENDING trước khi chuyển bất kỳ khoản tiền nào.
     * Nhờ vậy nếu ghi FAILED detail thất bại hoặc pod chết, owner chưa trả vẫn còn row
     * PENDING để retry và không thể bị bỏ quên.
     */
    private List<ProfitDistributionDetail> persistPayoutPlan(
            ProfitDistribution event,
            List<OwnerPayoutData> payoutPlan
    ) {
        List<ProfitDistributionDetail> saved = requiresNewTransaction.execute(status -> {
            ProfitDistribution managedDistribution = profitDistributionRepository.findById(event.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Profit distribution not found: " + event.getId()));
            List<ProfitDistributionDetail> details = new ArrayList<>(payoutPlan.size());
            for (OwnerPayoutData payout : payoutPlan) {
                ProfitDistributionDetail detail = new ProfitDistributionDetail();
                detail.setDistribution(managedDistribution);
                detail.setEvOwner(evOwnerRepository.getReferenceById(payout.getEvOwnerId()));
                detail.setMoneyAmount(payout.getPayoutAmount());
                detail.setCreditAmount(normalizeAmount(payout.getCreditContribution(), 6));
                detail.setEnergyAmount(normalizeAmount(payout.getEnergyContribution(), 6));
                detail.setStatus(DETAIL_PENDING);
                details.add(profitDistributionDetailRepository.save(detail));
            }
            return details;
        });
        if (saved == null) {
            throw new IllegalStateException("Could not persist payout plan for distribution #" + event.getId());
        }
        return saved;
    }

    /**
     * Chặn chạy lại toàn bộ report khi đã có owner được trả tiền ở distribution trước.
     * Trường hợp đó phải retry đúng distribution để không trả hai lần cho owner đã SUCCESS.
     *
     * <p>Distribution cũ không có emission_report_id được đối chiếu theo company + project
     * để tránh việc /share bỏ qua hoàn toàn và trả lần hai.</p>
     */
    private void rejectIfPayoutAlreadyStarted(EmissionReport report, Long companyUserId) {
        Optional<ProfitDistribution> previous =
                profitDistributionRepository.findFirstByEmissionReportIdOrderByIdDesc(report.getId());
        boolean legacy = false;
        if (previous.isEmpty() && report.getProject() != null) {
            previous = profitDistributionRepository
                    .findFirstByCompanyUserIdAndProjectIdAndEmissionReportIsNullOrderByIdDesc(
                            companyUserId, report.getProject().getId());
            legacy = previous.isPresent();
        }
        if (previous.isEmpty()) {
            return;
        }

        ProfitDistribution distribution = previous.get();
        if (distribution.getStatus() == ProfitDistributionStatus.PROCESSING
                || distribution.getStatus() == ProfitDistributionStatus.PENDING) {
            throw new BadRequestException("A payout for this report is already in progress (distribution #"
                    + distribution.getId() + ").");
        }

        // Chỉ chặn khi thực sự đã có owner nhận tiền. Một distribution COMPLETED
        // không có detail (không có contribution/payout) không được khoá report mãi mãi.
        if (profitDistributionDetailRepository.existsByDistributionIdAndStatusIgnoreCase(
                distribution.getId(), DETAIL_SUCCESS)) {
            String hint = legacy
                    ? " This is a legacy distribution without an emission-report link; retry it directly."
                    : "";
            throw new BadRequestException("Report " + report.getId() + " already has paid owners; retry distribution #"
                    + distribution.getId() + " instead of starting a new payout." + hint);
        }
    }

    // Lưu log lỗi khi không thể bắt đầu process payout (vd: ko tìm thấy user).
    // REQUIRES_NEW: phải commit được kể cả khi transaction payout bên ngoài rollback.
    private void saveFailedDetail(
            ProfitDistribution event,
            OwnerPayoutData payout,
            String errorMessage,
            Long existingDetailId
    ) {
        try {
            requiresNewTransaction.executeWithoutResult(status -> {
                ProfitDistributionDetail detail = null;
                if (existingDetailId != null) {
                    // Khóa row trước khi ghi FAILED: nếu worker khác vừa trả xong owner này
                    // và commit SUCCESS thì phải giữ SUCCESS, không được đè thành FAILED
                    // (đè FAILED sẽ mở đường cho một lần retry trả tiền lần hai).
                    detail = profitDistributionDetailRepository.findByIdForUpdate(existingDetailId).orElse(null);
                    if (detail == null
                            || detail.getDistribution() == null
                            || !Objects.equals(detail.getDistribution().getId(), event.getId())) {
                        // Không tạo dòng mới: sẽ làm summary của distribution bị cộng hai lần.
                        log.error("Cannot record failure for owner {}: detail {} is missing or belongs to another distribution",
                                payout.getEvOwnerId(), existingDetailId);
                        return;
                    }
                    if (isSettledDetail(detail)) {
                        log.info("Owner {} in distribution #{} is already {}; keeping settled detail",
                                payout.getEvOwnerId(), event.getId(), detail.getStatus());
                        return;
                    }
                }

                if (detail == null) {
                    EVOwner ownerRef = evOwnerRepository.getReferenceById(payout.getEvOwnerId());
                    detail = new ProfitDistributionDetail();
                    detail.setDistribution(event);
                    detail.setEvOwner(ownerRef);
                }

                detail.setMoneyAmount(payout.getPayoutAmount());
                detail.setCreditAmount(payout.getCreditContribution());
                detail.setEnergyAmount(payout.getEnergyContribution());
                detail.setStatus(DETAIL_FAILED);
                detail.setErrorMessage(errorMessage.substring(0, Math.min(errorMessage.length(), 254)));
                profitDistributionDetailRepository.save(detail);
            });
        } catch (Exception e) {
            log.error("CRITICAL: Failed to save failure detail for owner {}: {}", payout.getEvOwnerId(), e.getMessage());
        }
    }

    private String errorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
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

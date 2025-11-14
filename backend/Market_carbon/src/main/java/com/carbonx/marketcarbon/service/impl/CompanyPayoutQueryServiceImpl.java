package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.config.ProfitSharingProperties.ResolvedPolicy;
import com.carbonx.marketcarbon.dto.response.*;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CompanyPayoutQueryService;
import com.carbonx.marketcarbon.service.DynamicPricingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyPayoutQueryServiceImpl implements CompanyPayoutQueryService {

    private static final BigDecimal KG_PER_CREDIT = new BigDecimal("1000");
    private static final BigDecimal DEFAULT_EMISSION_FACTOR = new BigDecimal("0.4");

    private final ProfitSharingProperties profitSharingProperties;
    private final CompanyRepository companyRepository;
    private final VehicleRepository vehicleRepository;
    private final EmissionReportDetailRepository emissionReportDetailRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final ProfitDistributionDetailRepository profitDistributionDetailRepository;
    private final UserRepository userRepository;
    private final EmissionReportRepository emissionReportRepository;
    private final DynamicPricingService dynamicPricingService;


    @Override
    public PayoutFormulaResponse getPayoutFormula() {
        Company company = requireCompanyAccess();
        ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());

        BigDecimal marketPricePerCredit = dynamicPricingService.getMarketPricePerCredit();
        BigDecimal kwhPerCreditFactor = dynamicPricingService.getKwhPerCreditFactor();

        // Tính giá payout thực tế (Giá thị trường * % của Owner)
        BigDecimal actualPayoutPricePerCredit = marketPricePerCredit
                .multiply(policy.getOwnerSharePct())
                .setScale(2, RoundingMode.HALF_UP);

        // Tính giá payout mỗi kWh (nếu cần)
        BigDecimal payoutPricePerKwh = BigDecimal.ZERO;
        if (kwhPerCreditFactor.compareTo(BigDecimal.ZERO) > 0) {
            payoutPricePerKwh = actualPayoutPricePerCredit.divide(kwhPerCreditFactor, 6, RoundingMode.HALF_UP);
        }

        log.info(
                "Preview Policy for companyId={}: source={}, pricingMode={}, ownerSharePct={}, minPayout={}, currency={}",
                company.getId(),
                policy.getSource(),
                policy.getPricingMode(),
                policy.getOwnerSharePct(),
                policy.getMinPayout(),
                policy.getCurrency()
        );
        log.info(
                "Dynamic Prices (from In-Memory Bean): MarketPrice/Credit={}, KwhFactor={}, Calculated Payout/Credit={}",
                marketPricePerCredit,
                kwhPerCreditFactor,
                actualPayoutPricePerCredit
        );

        return PayoutFormulaResponse.builder()
                .pricingMode(policy.getPricingMode())
                .unitPrice(actualPayoutPricePerCredit)
                .minPayout(policy.getMinPayout())
                .unitPricePerKwh(payoutPricePerKwh)
                .unitPricePerCredit(actualPayoutPricePerCredit)
                .currency(policy.getCurrency())
                .build();
    }


    @Override
    public PageResponse<List<CompanyEVOwnerSummaryResponse>> listCompanyOwners(String period,
                                                                               int page,
                                                                               int size,
                                                                               String search) {
        Company company = requireCompanyAccess();
        if (!StringUtils.hasText(period)) {
            throw new BadRequestException("Period is required in format YYYY-MM");
        }
        if (size <= 0) { size = 20; }
        if (page < 0) { page = 0; }

        Map<Long, OwnerAggregation> aggregations = buildOwnerAggregations(company, period.trim());
        List<OwnerAggregation> filtered = new ArrayList<>(aggregations.values());

        if (StringUtils.hasText(search)) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);
            filtered = filtered.stream()
                    .filter(a -> matchesSearch(a, keyword))
                    .collect(Collectors.toList());
        }

        filtered.sort(Comparator.comparing(OwnerAggregation::getOwnerName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        int totalItems = filtered.size();
        int fromIndex = Math.min(page * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<CompanyEVOwnerSummaryResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(OwnerAggregation::toResponseWithoutPayout)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);

        return PageResponse.<List<CompanyEVOwnerSummaryResponse>>builder()
                .pageNo(page)
                .pageSize(size)
                .totalPages(totalPages)
                .items(items)
                .build();
    }

    @Override
    public CompanyReportOwnersResponse listCompanyOwnersForReport(Long reportId,
                                                                  int page,
                                                                  int size,
                                                                  String sort,
                                                                  String formula,
                                                                  BigDecimal pricePerCreditOverride,
                                                                  BigDecimal kwhToCreditFactorOverride,
                                                                  BigDecimal ownerSharePctOverride,
                                                                  int scale) {
        Company company = requireCompanyAccess();
        if (page < 0) { page = 0; }
        if (size <= 0) { size = 20; }
        if (scale < 0 || scale > 6) {
            throw new BadRequestException("Scale must be between 0 and 6");
        }

        ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
        FormulaMode formulaMode = FormulaMode.from(formula);

        // Lấy Payout % (ưu tiên override)
        BigDecimal ownerSharePct = resolveOwnerSharePct(ownerSharePctOverride, policy);

        // Lấy giá Payout (đã nhân %) (ưu tiên override)
        BigDecimal actualPayoutPricePerCredit = resolvePricePerCredit(pricePerCreditOverride, policy, ownerSharePct);

        // Lấy hệ số quy đổi (ưu tiên override)
        BigDecimal kwhPerCreditFactor = resolveKwhToCreditFactor(kwhToCreditFactorOverride);

        validateFormulaParameters(formulaMode, actualPayoutPricePerCredit, kwhPerCreditFactor, ownerSharePct);

        EmissionReport report = emissionReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission report not found"));
        if (report.getSeller() == null || !Objects.equals(report.getSeller().getId(), company.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<EmissionReportDetail> details = emissionReportDetailRepository.findByReport_Id(reportId);
        Map<Long, OwnerAggregation> aggregationMap = buildOwnerAggregations(company.getId(), details);
        List<OwnerAggregation> relevantAggregations = aggregationMap.values().stream()
                .filter(OwnerAggregation::hasContribution)
                .toList();

        if (relevantAggregations.isEmpty()) {
            PageResponse<List<CompanyPayoutSummaryItemResponse>> emptyPage = PageResponse.<List<CompanyPayoutSummaryItemResponse>>builder()
                    .pageNo(page)
                    .pageSize(size)
                    .totalPages(0)
                    .items(Collections.emptyList())
                    .build();

            CompanyPayoutSummaryResponse emptySummary = CompanyPayoutSummaryResponse.builder()
                    .items(Collections.emptyList())
                    .pageTotalPayout(BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP))
                    .grandTotalPayout(BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP))
                    .totalEnergyKwh(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                    .totalCredits(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                    .ownersCount(0)
                    .build();
            return CompanyReportOwnersResponse.builder()
                    .page(emptyPage)
                    .summary(emptySummary)
                    .build();
        }

        List<OwnerPayoutView> ownerViews = new ArrayList<>();
        BigDecimal grandTotalPayout = BigDecimal.ZERO;
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCreditsTCO2e = BigDecimal.ZERO;

        for (OwnerAggregation aggregation : relevantAggregations) {
            BigDecimal energy = aggregation.getEnergy().setScale(6, RoundingMode.HALF_UP);
            BigDecimal creditsTCO2e = aggregation.getCredits().setScale(6, RoundingMode.HALF_UP);

            BigDecimal payout = computePayout(
                    energy,
                    creditsTCO2e,
                    formulaMode,
                    actualPayoutPricePerCredit,
                    kwhPerCreditFactor,
                    scale
            );

            ownerViews.add(new OwnerPayoutView(aggregation, energy, creditsTCO2e, payout, ownerSharePct));
            grandTotalPayout = grandTotalPayout.add(payout);
            totalEnergy = totalEnergy.add(energy);
            totalCreditsTCO2e = totalCreditsTCO2e.add(creditsTCO2e);
        }

        ownerViews.sort(resolveComparator(sort));

        int totalOwners = ownerViews.size();
        int fromIndex = Math.min(page * size, totalOwners);
        int toIndex = Math.min(fromIndex + size, totalOwners);
        List<OwnerPayoutView> pageSlice = ownerViews.subList(fromIndex, toIndex);

        List<CompanyPayoutSummaryItemResponse> pageItems = pageSlice.stream()
                .map(view -> view.toResponse(scale))
                .toList();

        BigDecimal pageTotalPayout = pageSlice.stream()
                .map(OwnerPayoutView::getPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(scale, RoundingMode.HALF_UP);

        int totalPages = size == 0 ? 0 : (int) Math.ceil(totalOwners / (double) size);

        PageResponse<List<CompanyPayoutSummaryItemResponse>> pageResponse = PageResponse.<List<CompanyPayoutSummaryItemResponse>>builder()
                .pageNo(page)
                .pageSize(size)
                .totalPages(totalPages)
                .items(pageItems)
                .build();

        CompanyPayoutSummaryResponse summary = CompanyPayoutSummaryResponse.builder()
                .items(pageItems)
                .pageTotalPayout(pageTotalPayout)
                .grandTotalPayout(grandTotalPayout.setScale(scale, RoundingMode.HALF_UP))
                .totalEnergyKwh(totalEnergy.setScale(6, RoundingMode.HALF_UP))
                .totalCredits(totalCreditsTCO2e.setScale(6, RoundingMode.HALF_UP))
                .ownersCount(totalOwners)
                .build();

        return CompanyReportOwnersResponse.builder()
                .page(pageResponse)
                .summary(summary)
                .build();
    }

    @Override
    public CompanyPayoutSummaryResponse  getDistributionSummary( Long distributionId) {
        Company company = requireCompanyAccess();
        ProfitDistribution distribution = profitDistributionRepository.findById(distributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Profit distribution not found"));
        if (!Objects.equals(distribution.getCompanyUser().getId(), company.getUser().getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<ProfitDistributionDetail> details = profitDistributionDetailRepository
                .findByDistributionIdWithOwner(distributionId);

        List<CompanyPayoutSummaryItemResponse> items = new ArrayList<>();
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;
        long ownersPaid = 0;

        for (ProfitDistributionDetail detail : details) {
            if (detail.getEvOwner() == null) {
                continue;
            }
            long vehiclesCount = vehicleRepository.countByEvOwner_IdAndCompany_Id(detail.getEvOwner().getId(), company.getId());
            BigDecimal energy = optional(detail.getEnergyAmount(), 6);
            BigDecimal creditsKg = optional(detail.getCreditAmount(), 6);
            BigDecimal creditsTCO2e = creditsKg.divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
            BigDecimal amount = optional(detail.getMoneyAmount(), 2);

            totalEnergy = totalEnergy.add(energy);
            totalCredits = totalCredits.add(creditsTCO2e);
            totalPayout = totalPayout.add(amount);
            if ("SUCCESS".equalsIgnoreCase(detail.getStatus())) {
                ownersPaid++;
            }

            items.add(CompanyPayoutSummaryItemResponse.builder()
                    .ownerId(detail.getEvOwner().getId())
                    .ownerName(detail.getEvOwner().getName())
                    .email(detail.getEvOwner().getEmail())
                    .phone(detail.getEvOwner().getPhone())
                    .vehiclesCount(vehiclesCount)
                    .energyKwh(energy)
                    .credits(creditsTCO2e)
                    .amountUsd(amount)
                    .status(detail.getStatus())
                    .build());
        }

        return CompanyPayoutSummaryResponse.builder()
                .items(items)
                .ownersCount(ownersPaid)
                .totalEnergyKwh(totalEnergy)
                .totalCredits(totalCredits.setScale(6, RoundingMode.HALF_UP))
                .grandTotalPayout(totalPayout)
                .build();
    }

    private Map<Long, OwnerAggregation> buildOwnerAggregations(Company company, String period) {
        List<EmissionReportDetail> details = emissionReportDetailRepository
                .findByCompanyIdAndPeriod(company.getId(), period);
        return buildOwnerAggregations(company.getId(), details);
    }

    private Map<Long, OwnerAggregation> buildOwnerAggregations(Long companyId, List<EmissionReportDetail> details) {
        OwnerAggregationContext context = prepareOwnerAggregation(companyId);
        if (details == null || details.isEmpty()) {
            return context.getOwnersById();
        }
        for (EmissionReportDetail detail : details) {
            if (detail.getCompanyId() != null && !Objects.equals(detail.getCompanyId(), companyId)) {
                continue;
            }
            String normalizedPlate = normalizePlate(detail.getVehiclePlate());
            if (!StringUtils.hasText(normalizedPlate)) {
                continue;
            }
            OwnerAggregation aggregation = context.getOwnersByPlate().get(normalizedPlate);
            if (aggregation == null) {
                continue;
            }
            BigDecimal energy = optional(detail.getTotalEnergy(), 6);
            BigDecimal credits = resolveCredits(detail, energy);
            aggregation.addEnergy(energy);
            aggregation.addCredits(credits);
        }
        return context.getOwnersById();
    }

    private OwnerAggregationContext prepareOwnerAggregation(Long companyId) {
        List<Vehicle> vehicles = vehicleRepository.findByCompanyId(companyId);
        Map<Long, OwnerAggregation> ownersById = new HashMap<>();
        Map<String, OwnerAggregation> ownersByPlate = new HashMap<>();

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getEvOwner() == null) {
                continue;
            }
            OwnerAggregation aggregation = ownersById.computeIfAbsent(
                    vehicle.getEvOwner().getId(),
                    id -> new OwnerAggregation(vehicle.getEvOwner().getId(),
                            vehicle.getEvOwner().getName(),
                            vehicle.getEvOwner().getEmail(),
                            vehicle.getEvOwner().getPhone()));
            aggregation.incrementVehicleCount();
            if (StringUtils.hasText(vehicle.getPlateNumber())) {
                ownersByPlate.put(normalizePlate(vehicle.getPlateNumber()), aggregation);
            }
        }
        return new OwnerAggregationContext(ownersById, ownersByPlate);
    }

    private boolean matchesSearch(OwnerAggregation aggregation, String keyword) {
        return (aggregation.getOwnerName() != null && aggregation.getOwnerName().toLowerCase(Locale.ROOT).contains(keyword))
                || (aggregation.getEmail() != null && aggregation.getEmail().toLowerCase(Locale.ROOT).contains(keyword))
                || (aggregation.getPhone() != null && aggregation.getPhone().toLowerCase(Locale.ROOT).contains(keyword));
    }

    private String normalizePlate(String plate) {
        return plate == null ? null : plate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private BigDecimal resolveCredits(EmissionReportDetail detail, BigDecimal fallbackEnergy) {
        if (detail.getCo2Kg() != null && detail.getCo2Kg().compareTo(BigDecimal.ZERO) > 0) {
            return detail.getCo2Kg().divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
        }
        if (fallbackEnergy != null && fallbackEnergy.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackEnergy.multiply(DEFAULT_EMISSION_FACTOR)
                    .divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolvePricePerCredit(BigDecimal override,
                                             ResolvedPolicy policy,
                                             BigDecimal ownerSharePct) {
        if (override != null) {
            return override;
        }
        BigDecimal marketPricePerCredit = dynamicPricingService.getMarketPricePerCredit();
        return marketPricePerCredit
                .multiply(ownerSharePct)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveOwnerSharePct(BigDecimal override, ResolvedPolicy policy) {
        if (override != null) {
            return override;
        }
        return Optional.ofNullable(policy.getOwnerSharePct())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal resolveKwhToCreditFactor(BigDecimal override) {
        if (override != null) {
            return override;
        }
        return dynamicPricingService.getKwhPerCreditFactor();
    }

    private void validateFormulaParameters(FormulaMode mode,
                                           BigDecimal pricePerCredit,
                                           BigDecimal kwhToCreditFactor,
                                           BigDecimal ownerSharePct) {
        if (pricePerCredit == null || pricePerCredit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("pricePerCredit must be zero or greater");
        }
        if (ownerSharePct == null
                || ownerSharePct.compareTo(BigDecimal.ZERO) < 0
                || ownerSharePct.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException("ownerSharePct must be between 0 and 1");
        }
        if (mode == FormulaMode.ENERGY) {
            if (kwhToCreditFactor == null || kwhToCreditFactor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("kwhToCreditFactor must be greater than zero when formula is ENERGY");
            }
        }
    }

    private BigDecimal computePayout(BigDecimal energyKwh,
                                     BigDecimal creditsTCO2e,
                                     FormulaMode formulaMode,
                                     BigDecimal actualPayoutPricePerCredit,
                                     BigDecimal kwhPerCreditFactor,
                                     int scale) {

        BigDecimal base;

        if (formulaMode == FormulaMode.ENERGY) {
            if (kwhPerCreditFactor.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("KWH_PER_CREDIT_FACTOR is zero, cannot calculate KWH payout. Defaulting to CREDIT mode.");
                base = creditsTCO2e.multiply(actualPayoutPricePerCredit);
            } else {
                BigDecimal payoutPricePerKwh = actualPayoutPricePerCredit.divide(kwhPerCreditFactor, 6, RoundingMode.HALF_UP);
                base = energyKwh.multiply(payoutPricePerKwh);
            }
        }
        else {
            base = creditsTCO2e.multiply(actualPayoutPricePerCredit);
        }

        return base.setScale(scale, RoundingMode.HALF_UP);
    }

    private Comparator<OwnerPayoutView> resolveComparator(String sort) {
        String field = "ownerName";
        String direction = "asc";
        if (StringUtils.hasText(sort)) {
            String[] tokens = sort.split(",");
            if (tokens.length > 0 && StringUtils.hasText(tokens[0])) {
                field = tokens[0].trim();
            }
            if (tokens.length > 1 && StringUtils.hasText(tokens[1])) {
                direction = tokens[1].trim();
            }
        }

        Comparator<OwnerPayoutView> comparator;
        switch (field) {
            case "totalEnergyKwh" -> comparator = Comparator.comparing(OwnerPayoutView::getEnergy);
            case "totalCredits" -> comparator = Comparator.comparing(OwnerPayoutView::getCredits);
            case "payoutAmount" -> comparator = Comparator.comparing(OwnerPayoutView::getPayout);
            case "vehiclesCount" -> comparator = Comparator.comparingLong(view -> view.getAggregation().getVehiclesCount());
            default -> comparator = Comparator.comparing(
                    view -> view.getAggregation().getOwnerName(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private enum FormulaMode {
        CREDITS,
        ENERGY;

        private static FormulaMode from(String value) {
            if (!StringUtils.hasText(value)) {
                return CREDITS;
            }
            try {
                return FormulaMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unsupported formula mode: " + value);
            }
        }
    }

    @Getter
    private static class OwnerAggregationContext {
        private final Map<Long, OwnerAggregation> ownersById;
        private final Map<String, OwnerAggregation> ownersByPlate;

        private OwnerAggregationContext(Map<Long, OwnerAggregation> ownersById,
                                        Map<String, OwnerAggregation> ownersByPlate) {
            this.ownersById = ownersById;
            this.ownersByPlate = ownersByPlate;
        }
    }

    @Getter
    private static class OwnerPayoutView {
        private final OwnerAggregation aggregation;
        private final BigDecimal energy;
        private final BigDecimal credits; // (Đây là tCO2e)
        private final BigDecimal payout;
        private final BigDecimal ownerSharePct;

        private OwnerPayoutView(OwnerAggregation aggregation,
                                BigDecimal energy,
                                BigDecimal credits,
                                BigDecimal payout,
                                BigDecimal ownerSharePct) {
            this.aggregation = aggregation;
            this.energy = energy;
            this.credits = credits;
            this.payout = payout;
            this.ownerSharePct = ownerSharePct;
        }

        private CompanyPayoutSummaryItemResponse toResponse(int payoutScale) {
            // Hiển thị số credit gốc (chưa nhân %)
            BigDecimal originalCredits = credits.setScale(6, RoundingMode.HALF_UP);

            return CompanyPayoutSummaryItemResponse.builder()
                    .ownerId(aggregation.getOwnerId())
                    .ownerName(aggregation.getOwnerName())
                    .email(aggregation.getEmail())
                    .phone(aggregation.getPhone())
                    .vehiclesCount(aggregation.getVehiclesCount())
                    .energyKwh(energy)
                    .credits(originalCredits) // Hiển thị credit gốc
                    .amountUsd(payout.setScale(payoutScale, RoundingMode.HALF_UP))
                    .status("PREVIEW")
                    .build();
        }
    }


    private Company requireCompanyAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findByEmail(authentication.getName());
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        if (!Objects.equals(company.getUser().getId(), user.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return company;
    }

    private BigDecimal optional(BigDecimal value, int scale) {
        return Optional.ofNullable(value)
                .orElse(BigDecimal.ZERO)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    @Getter
    private static class OwnerAggregation {
        private final Long ownerId;
        private final String ownerName;
        private final String email;
        private final String phone;
        private long vehiclesCount;
        private BigDecimal energy = BigDecimal.ZERO;
        private BigDecimal credits = BigDecimal.ZERO; // (Đây là tCO2e)

        private OwnerAggregation(Long ownerId, String ownerName, String email, String phone) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.email = email;
            this.phone = phone;
        }

        private void incrementVehicleCount() {
            this.vehiclesCount++;
        }

        private void addEnergy(BigDecimal value) {
            if (value == null) {
                return;
            }
            energy = energy.add(value);
        }

        private void addCredits(BigDecimal value) {
            if (value == null) {
                return;
            }
            credits = credits.add(value);
        }

        private CompanyEVOwnerSummaryResponse toResponseWithoutPayout() {
            return CompanyEVOwnerSummaryResponse.builder()
                    .ownerId(ownerId)
                    .ownerName(ownerName)
                    .email(email)
                    .phone(phone)
                    .vehiclesCount(vehiclesCount)
                    .totalEnergyKwh(energy.setScale(6, RoundingMode.HALF_UP))
                    .payoutAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalCredits(credits.setScale(6, RoundingMode.HALF_UP))
                    .build();
        }
        private boolean hasContribution() {
            return energy.compareTo(BigDecimal.ZERO) > 0 || credits.compareTo(BigDecimal.ZERO) > 0;
        }
    }

}

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import com.carbonx.marketcarbon.dto.response.*;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CompanyPayoutQueryService;
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


    @Override
    public PayoutFormulaResponse getPayoutFormula() {
        //  B1: xac thuc user dang nhap va lay company
        Company company = requireCompanyAccess();
        // B2: doc chinh sach chia loi nhuan tu config theo company
        ProfitSharingProperties.ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
        // B3: dong goi du lieu thanh response gui ve frontend
        return PayoutFormulaResponse.builder()
                .pricingMode(policy.getPricingMode())
                .unitPrice(policy.getUnitPrice())
                .minPayout(policy.getMinPayout())
                .unitPricePerKwh(policy.getUnitPricePerKwh())
                .unitPricePerCredit(policy.getUnitPricePerCredit())
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
        if (size <= 0) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }

        //  B1: gom tong hop dong gop cua tung owner theo ky
        Map<Long, OwnerAggregation> aggregations = buildOwnerAggregations(company, period.trim());

        List<OwnerAggregation> filtered = new ArrayList<>(aggregations.values());
        //  B2: neu co tu khoa thi loc theo ten email hoac so dien thoai
        if (StringUtils.hasText(search)) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);
            filtered = filtered.stream()
                    .filter(a -> matchesSearch(a, keyword))
                    .collect(Collectors.toList());
        }

        //B3 sap xep theo ten ev owner va ko phan biet chu hoa
        filtered.sort(Comparator.comparing(OwnerAggregation::getOwnerName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        // B4 tim phan trang
        int totalItems = filtered.size();
        int fromIndex = Math.min(page * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<CompanyEVOwnerSummaryResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(OwnerAggregation::toResponseWithoutPayout)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);

        // B5 tra ve dung page réponse
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
        // B1: xac thuc va lay company tu user dang nhap
        Company company = requireCompanyAccess();
        // B2: chuan hoa tham so phan trang va scale dau vao
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (scale < 0 || scale > 6) {
            throw new BadRequestException("Scale must be between 0 and 6");
        }

        // B3: doc chinh sach co ban tu config de lam gia tri fallback
        ProfitSharingProperties.ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
        FormulaMode formulaMode = FormulaMode.from(formula);

        BigDecimal pricePerCredit = resolvePricePerCredit(pricePerCreditOverride, policy);
        BigDecimal ownerSharePct = resolveOwnerSharePct(ownerSharePctOverride);
        BigDecimal kwhToCreditFactor = resolveKwhToCreditFactor(kwhToCreditFactorOverride);

        // B4: validate cac tham so cong thuc truoc khi tinh toan
        validateFormulaParameters(formulaMode, pricePerCredit, kwhToCreditFactor, ownerSharePct);

        // B5: dam bao report ton tai va thuoc ve company dang dang nhap
        EmissionReport report = emissionReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission report not found"));
        if (report.getSeller() == null || !Objects.equals(report.getSeller().getId(), company.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // B6: lay chi tiet report va gom dong gop theo chu xe
        List<EmissionReportDetail> details = emissionReportDetailRepository.findByReport_Id(reportId);
        Map<Long, OwnerAggregation> aggregationMap = buildOwnerAggregations(company.getId(), details);
        List<OwnerAggregation> relevantAggregations = aggregationMap.values().stream()
                .filter(OwnerAggregation::hasContribution)
                .toList();
        if (relevantAggregations.isEmpty()) {
            // B6.1: tra ve trang rong va tong bang 0 neu khong co dong gop
            PageResponse<List<CompanyPayoutSummaryItemResponse>> emptyPage = PageResponse.<List<CompanyPayoutSummaryItemResponse>>builder() // <-- SỬA
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

        // B7: tinh toan payout cho tung chu xe va cong don tong
        List<OwnerPayoutView> ownerViews = new ArrayList<>();
        BigDecimal grandTotalPayout = BigDecimal.ZERO;
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (OwnerAggregation aggregation : aggregationMap.values()) {
            BigDecimal energy = aggregation.getEnergy().setScale(6, RoundingMode.HALF_UP);
            BigDecimal credits = aggregation.getCredits().setScale(6, RoundingMode.HALF_UP);
            BigDecimal payout = computePayout(energy, credits, formulaMode, pricePerCredit, kwhToCreditFactor, ownerSharePct, scale);
            ownerViews.add(new OwnerPayoutView(aggregation, energy, credits, payout, ownerSharePct));
            grandTotalPayout = grandTotalPayout.add(payout);
            totalEnergy = totalEnergy.add(energy);
            totalCredits = totalCredits.add(credits);
        }

        // B8: sap xep danh sach theo truong duoc yeu cau
        ownerViews.sort(resolveComparator(sort));

        // B9: ap dung phan trang tren danh sach da sap xep
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

        // THAY ĐỔI 2: PageResponse cũng phải thay đổi kiểu
        PageResponse<List<CompanyPayoutSummaryItemResponse>> pageResponse = PageResponse.<List<CompanyPayoutSummaryItemResponse>>builder() // <--- Dòng mới
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
                .totalCredits(totalCredits.setScale(6, RoundingMode.HALF_UP))
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

        List<CompanyPayoutSummaryItemResponse> items = new ArrayList<>(); // <--- Sửa

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
            BigDecimal credits = optional(detail.getCreditAmount(), 6);
            BigDecimal amount = optional(detail.getMoneyAmount(), 2);

            totalEnergy = totalEnergy.add(energy);
            totalCredits = totalCredits.add(credits);
            totalPayout = totalPayout.add(amount);
            if ("SUCCESS".equalsIgnoreCase(detail.getStatus())) {
                ownersPaid++;
            }

            items.add(CompanyPayoutSummaryItemResponse.builder() // <--- Sửa
                    .ownerId(detail.getEvOwner().getId())
                    .ownerName(detail.getEvOwner().getName())
                    .email(detail.getEvOwner().getEmail())
                    .phone(detail.getEvOwner().getPhone())
                    .vehiclesCount(vehiclesCount)
                    .energyKwh(energy) // <--- Lỗi (2.1) của bạn bây giờ đã được sửa vì builder này có 'energyKwh'
                    .credits(credits)
                    .amountUsd(amount) // <--- Trường này cũng có trong builder đúng
                    .status(detail.getStatus()) // <--- Trường này cũng có trong builder đúng
                    .build());
        }

        return CompanyPayoutSummaryResponse.builder()
                .items(items)
                .ownersCount(ownersPaid)
                .totalEnergyKwh(totalEnergy)
                .totalCredits(totalCredits)
                .grandTotalPayout(totalPayout)
                .build();
    }

    private Map<Long, OwnerAggregation> buildOwnerAggregations(Company company, String period) {
        List<EmissionReportDetail> details = emissionReportDetailRepository
                .findByCompanyIdAndPeriod(company.getId(), period);
        // B2: gom dong gop tu danh sach chi tiet vua doc
        return buildOwnerAggregations(company.getId(), details);
    }

    private Map<Long, OwnerAggregation> buildOwnerAggregations(Long companyId, List<EmissionReportDetail> details) {
        // B1: tao context gom map chu xe va map bien so
        OwnerAggregationContext context = prepareOwnerAggregation(companyId);
        if (details == null || details.isEmpty()) {
            return context.getOwnersById();
        }
        // B2: cong don thong tin chi tiet vao tung chu xe tuong ung
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
        // B1: lay danh sach xe thuoc company kem thong tin chu xe
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

    private BigDecimal resolvePricePerCredit(BigDecimal override, ProfitSharingProperties.ResolvedPolicy policy) {
        // B1: uu tien gia tri override neu client truyen len
        if (override != null) {
            return override;
        }
        // B2: su dung don gia credit tu policy neu co
        if (policy.getUnitPricePerCredit() != null) {
            return policy.getUnitPricePerCredit();
        }
        // B3: fallback sang don gia hieu luc
        return Optional.ofNullable(policy.getUnitPrice())
                .orElseThrow(() -> new BadRequestException("Price per credit is not configured"));
    }

    private BigDecimal resolveOwnerSharePct(BigDecimal override) {
        // B1: neu client truyen len thi dung gia tri do
        if (override != null) {
            return override;
        }
        Company company = requireCompanyAccess();
        // B2: config  100% chia cho chu xe
        ProfitSharingProperties.ResolvedPolicy policy = profitSharingProperties.resolveForCompany(company.getId());
        return Optional.ofNullable(policy.getOwnerSharePct())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal resolveKwhToCreditFactor(BigDecimal override) {
        // B1: neu client override thi dung gia tri do
        if (override != null) {
            return override;
        }
        // B2: mac dinh dung he so phat thai co ban chia 1000 de doi sang credit
        return DEFAULT_EMISSION_FACTOR.divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);
    }

    private void validateFormulaParameters(FormulaMode mode,
                                           BigDecimal pricePerCredit,
                                           BigDecimal kwhToCreditFactor,
                                           BigDecimal ownerSharePct) {
        // B1: gia credit phai lon hon 0
        if (pricePerCredit == null || pricePerCredit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("pricePerCredit must be greater than zero");
        }
        // B2: ti le chia cho chu xe phai trong khoang [0,1]
        if (ownerSharePct == null
                || ownerSharePct.compareTo(BigDecimal.ZERO) < 0
                || ownerSharePct.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException("ownerSharePct must be between 0 and 1");
        }
        // B3: neu tinh theo energy thi he so quy doi phai > 0
        if (mode == FormulaMode.ENERGY) {
            if (kwhToCreditFactor == null || kwhToCreditFactor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("kwhToCreditFactor must be greater than zero when formula is ENERGY");
            }
        }
    }

    private BigDecimal computePayout(BigDecimal energy,
                                     BigDecimal credits,
                                     FormulaMode formulaMode,
                                     BigDecimal pricePerCredit,
                                     BigDecimal kwhToCreditFactor,
                                     BigDecimal ownerSharePct,
                                     int scale) {
        // B1: tinh gia tri credit can nhan theo mode
        BigDecimal base;
        if (formulaMode == FormulaMode.CREDITS) {
            base = credits.multiply(pricePerCredit);
        } else {
            base = energy.multiply(kwhToCreditFactor)
                    .multiply(pricePerCredit);
        }
        // B2: ap dung ty le chia cho chu xe
        BigDecimal payout = base.multiply(ownerSharePct);
        // B3: chuan hoa scale theo tham so dau vao
        return payout.setScale(scale, RoundingMode.HALF_UP);
    }

    private Comparator<OwnerPayoutView> resolveComparator(String sort) {
        // B1: mac dinh sap xep theo ten chu xe tang dan
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
        private final BigDecimal credits;
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
            BigDecimal creditAfterShare = credits.multiply(ownerSharePct)
                    .setScale(6, RoundingMode.HALF_UP);

            return CompanyPayoutSummaryItemResponse.builder()
                    .ownerId(aggregation.getOwnerId())
                    .ownerName(aggregation.getOwnerName())
                    .email(aggregation.getEmail())
                    .phone(aggregation.getPhone())
                    .vehiclesCount(aggregation.getVehiclesCount())
                    .energyKwh(energy)
                    .credits(creditAfterShare)
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
        private BigDecimal credits = BigDecimal.ZERO;

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

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse;
import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse.StatusCount;
import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse.ProjectCount;
import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse.VintageCount;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.MyCreditInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyCreditInventoryServiceImpl implements MyCreditInventoryService {

    private final CarbonCreditRepository creditRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;

    // Lấy companyId của user hiện tại
    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        return companyRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public CreditInventorySummaryResponse getMyInventorySummary() {
        Long companyId = currentCompanyId();

        //  Query SUM(amount) cho các trạng thái (trừ EXPIRED)
        var byStatusRaw = creditRepo.sumAmountByStatusExcluding(companyId, CreditStatus.EXPIRED);
        var byProjectRaw = creditRepo.sumAmountByProjectExcluding(companyId, CreditStatus.EXPIRED);
        var byVintageRaw = creditRepo.sumAmountByVintageExcluding(companyId, CreditStatus.EXPIRED);

        //  COUNT(id) riêng cho RETIRED
        long retiredCount = creditRepo.countByCompanyIdAndStatus(companyId, CreditStatus.RETIRED);

        //  ISSUED trong 30 ngày gần nhất
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(30);
        long issuedVirtual = creditRepo.sumRecentlyIssued(companyId, cutoffDate);

        //  Biến đếm
        long available = 0;
        long listed = 0; // reserved
        long sold = 0;

        List<StatusCount> byStatus = new ArrayList<>();

        //  Phân loại theo trạng thái
        for (Object[] row : byStatusRaw) {
            String status = String.valueOf(row[0]);
            long sum = ((Number) row[1]).longValue();
            boolean addToList = true;

            switch (status) {
                case "AVAILABLE" -> available = sum;
                case "LISTED"    -> listed = sum;
                case "SOLD"      -> sold = sum;
                case "RETIRED"   -> addToList = false;
            }

            if (addToList) {
                byStatus.add(StatusCount.builder()
                        .status(status)
                        .count(sum)
                        .build());
            }
        }

        //  Thêm RETIRED riêng
        if (retiredCount > 0) {
            byStatus.add(StatusCount.builder()
                    .status("RETIRED")
                    .count(retiredCount)
                    .build());
        }

        //  Thêm ISSUED (30 ngày gần đây)
        if (issuedVirtual > 0) {
            byStatus.add(StatusCount.builder()
                    .status("ISSUED")
                    .count(issuedVirtual)
                    .build());
        }

        //  Tính BUYED = tổng tín chỉ được mua từ công ty khác
        long buyed = creditRepo.sumBuyedAmount(companyId);

        //  byProject
        List<ProjectCount> byProject = new ArrayList<>();
        for (Object[] row : byProjectRaw) {
            byProject.add(ProjectCount.builder()
                    .projectId((Long) row[0])
                    .projectTitle((String) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }

        //  byVintage
        List<VintageCount> byVintage = new ArrayList<>();
        for (Object[] row : byVintageRaw) {
            Integer vintageYear = row[0] != null ? ((Number) row[0]).intValue() : null;
            long sum = ((Number) row[1]).longValue();
            byVintage.add(VintageCount.builder()
                    .vintageYear(vintageYear)
                    .count(sum)
                    .build());
        }

        //  Trả về DTO
        return CreditInventorySummaryResponse.builder()
                .buyed(buyed)
                .issued(issuedVirtual)
                .available(available)
                .reserved(listed)
                .sold(sold)
                .retired(retiredCount)
                .byStatus(byStatus)
                .byProject(byProject)
                .byVintage(byVintage)
                .build();
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public long getMyAvailableBalance() {
        return creditRepo.sumAmountByCompany_IdAndStatus(currentCompanyId(), CreditStatus.AVAILABLE);
    }
}

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

        var byStatusRaw = creditRepo.sumAmountByStatusExcluding(companyId, CreditStatus.EXPIRED);
        var byProjectRaw = creditRepo.sumAmountByProjectExcluding(companyId, CreditStatus.EXPIRED);
        var byVintageRaw = creditRepo.sumAmountByVintageExcluding(companyId, CreditStatus.EXPIRED);

        // Chỉ số phụ (không cộng vào total)
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(30);
        long issuedVirtual = creditRepo.sumRecentlyIssued(companyId, cutoffDate);

        long available = 0, reserved = 0, sold = 0, retired = 0;
        List<StatusCount> byStatus = new ArrayList<>();

        for (Object[] row : byStatusRaw) {
            String status = String.valueOf(row[0]);
            long sum = ((Number) row[1]).longValue();

            switch (status) {
                case "AVAILABLE" -> available = sum;
                case "RESERVED"  -> reserved  = sum;
                case "SOLD"      -> sold      = sum;
                case "RETIRED"   -> retired   = sum;
            }

            byStatus.add(StatusCount.builder()
                    .status(status)
                    .count(sum)
                    .build());
        }

        // Thêm "ISSUED" (30 ngày gần nhất) như CHỈ SỐ PHỤ (không cộng vào total)
        if (issuedVirtual > 0) {
            byStatus.add(StatusCount.builder()
                    .status("ISSUED")
                    .count(issuedVirtual)
                    .build());
        }

        long total = available;

        List<ProjectCount> byProject = new ArrayList<>();
        for (Object[] row : byProjectRaw) {
            byProject.add(ProjectCount.builder()
                    .projectId((Long) row[0])
                    .projectTitle((String) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }

        List<VintageCount> byVintage = new ArrayList<>();
        for (Object[] row : byVintageRaw) {
            Integer vintageYear = row[0] != null ? ((Number) row[0]).intValue() : null;
            long sum = ((Number) row[1]).longValue();
            byVintage.add(VintageCount.builder()
                    .vintageYear(vintageYear)
                    .count(sum)
                    .build());
        }

        return CreditInventorySummaryResponse.builder()
                .total(total)
                .issued(issuedVirtual)     // chỉ số phụ
                .available(available)
                .reserved(reserved)
                .sold(sold)
                .retired(retired)
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

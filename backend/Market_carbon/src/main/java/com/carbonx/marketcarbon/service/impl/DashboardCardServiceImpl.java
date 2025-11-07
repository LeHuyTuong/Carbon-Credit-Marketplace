package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.dashboard.MonthlyCreditStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyReportStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.SummaryValue;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.repository.ProjectApplicationRepository;
import com.carbonx.marketcarbon.service.DashboardCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCardServiceImpl implements DashboardCardService {

    private final EmissionReportRepository reportRepo;
    private final CarbonCreditRepository creditRepo;
    private final CompanyRepository companyRepo;
    private final ProjectApplicationRepository projectAppRepo;

    @Override
    public SummaryValue getReportSummary() {
        return new SummaryValue(reportRepo.countAllReports());
    }

    @Override
    public SummaryValue getCreditSummary() {
        return new SummaryValue(creditRepo.countAllCredits());
    }

    @Override
    public SummaryValue getCompanySummary() {
        return new SummaryValue(companyRepo.countAllCompanies());
    }

    @Override
    public SummaryValue getProjectSummary() {
        return new SummaryValue(projectAppRepo.countAllProjects());
    }

    @Override
    public List<MonthlyReportStatusDto> getMonthlyReportStatus() {
        return reportRepo.countMonthlyReportStatusNative()
                .stream()
                .map(r -> new MonthlyReportStatusDto(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue()
                ))
                .toList();
    }

    @Override
    public List<MonthlyCreditStatusDto> getMonthlyCreditStatus() {
        List<Object[]> rows = creditRepo.countMonthlyCreditStatusNative();
        List<MonthlyCreditStatusDto> result = new ArrayList<>();

        for (Object[] row : rows) {
            result.add(new MonthlyCreditStatusDto(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue()
            ));
        }
        return result;
    }
}

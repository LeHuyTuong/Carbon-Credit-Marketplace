package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.dashboard.MonthlyCreditStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyProjectStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyReportStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.SummaryValue;
import java.util.List;

public interface DashboardCardService {
    SummaryValue getReportSummary();
    SummaryValue getCreditSummary();
    SummaryValue getCompanySummary();
    SummaryValue getProjectSummary();

    List<MonthlyReportStatusDto> getMonthlyReportStatus();
    public List<MonthlyCreditStatusDto> getMonthlyCreditStatus();

    public List<MonthlyProjectStatusDto> getMonthlyProjectStatus();
}
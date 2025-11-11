package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.*;

import java.util.List;

public interface CompanyPayoutQueryService {
    PayoutFormulaResponse getPayoutFormula();

    PageResponse<List<CompanyEVOwnerSummaryResponse>> listCompanyOwners(
                                                                        String period,
                                                                        int page,
                                                                        int size,
                                                                        String search);

    CompanyReportOwnersResponse listCompanyOwnersForReport(
                                                           Long reportId,
                                                           int page,
                                                           int size,
                                                           String sort,
                                                           int scale);

    CompanyPayoutSummaryResponse getDistributionSummary(Long distributionId);
}

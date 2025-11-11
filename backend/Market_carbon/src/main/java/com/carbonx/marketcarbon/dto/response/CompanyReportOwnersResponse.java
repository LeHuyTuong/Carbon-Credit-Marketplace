package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompanyReportOwnersResponse {

    private final PageResponse<List<CompanyPayoutSummaryItemResponse>> page; // <-- SỬA THÀNH DÒNG NÀY
    private final CompanyPayoutSummaryResponse summary;
}

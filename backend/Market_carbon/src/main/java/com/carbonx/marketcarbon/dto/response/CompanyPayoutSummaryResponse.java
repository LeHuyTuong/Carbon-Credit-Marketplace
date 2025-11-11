package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CompanyPayoutSummaryResponse {

    private final List<CompanyPayoutSummaryItemResponse> items; // <--- Sửa thành loại này

    private final BigDecimal pageTotalPayout;
    private final BigDecimal grandTotalPayout;
    private final BigDecimal totalEnergyKwh;
    private final BigDecimal totalCredits;
    private final long ownersCount;
    private final String currency;
}

package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CompanyPayoutSummaryItemResponse {
    private final Long ownerId;
    private final String ownerName;
    private final String email;
    private final String phone;
    private final long vehiclesCount;
    private final BigDecimal energyKwh;
    private final BigDecimal credits;
    private final BigDecimal amountVnd;
    private final String status;
}

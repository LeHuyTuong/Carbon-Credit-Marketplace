package com.carbonx.marketcarbon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Builder
public class CompanyEVOwnerSummaryResponse {

    private final Long ownerId;
    private final String ownerName;
    private final String email;
    private final String phone;
    private final long vehiclesCount;
    private final BigDecimal totalEnergyKwh;
    private final BigDecimal totalCredits;
    private final BigDecimal payoutAmount;
}

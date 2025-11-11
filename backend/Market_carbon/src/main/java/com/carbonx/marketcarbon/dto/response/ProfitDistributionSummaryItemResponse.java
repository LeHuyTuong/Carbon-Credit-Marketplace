package com.carbonx.marketcarbon.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;


@Getter
@RequiredArgsConstructor
public class ProfitDistributionSummaryItemResponse {
    private final Long ownerId;
    private final String ownerName;
    private final String email;
    private final String phone;
    private final long vehiclesCount;
    private final BigDecimal energyKwh;
    private final BigDecimal credits;
    private final BigDecimal amountUsd;
    private final String currency;
    private final String status;

    public static ProfitDistributionSummaryItemResponse of(Long ownerId,
                                                           String ownerName,
                                                           String email,
                                                           String phone,
                                                           long vehiclesCount,
                                                           BigDecimal energyKwh,
                                                           BigDecimal credits,
                                                           BigDecimal amountUsd,
                                                           String  currency,
                                                           String status) {
        return new ProfitDistributionSummaryItemResponse(ownerId,
                ownerName,
                email,
                phone,
                vehiclesCount,
                energyKwh,
                credits,
                amountUsd,
                currency,
                status);
    }
}

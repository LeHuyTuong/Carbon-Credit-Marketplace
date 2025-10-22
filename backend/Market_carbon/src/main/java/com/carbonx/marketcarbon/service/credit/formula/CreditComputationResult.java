package com.carbonx.marketcarbon.service.credit.formula;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CreditComputationResult {
    // tổng tCO2e sau điều chỉnh, scale=3, DOWN
    private final BigDecimal totalTco2e;
    // số credit = floor(totalTco2e)
    private final int creditsCount;
    // phần dư < 1 tCO2e, scale=3, DOWN
    private final BigDecimal residualTco2e;
}

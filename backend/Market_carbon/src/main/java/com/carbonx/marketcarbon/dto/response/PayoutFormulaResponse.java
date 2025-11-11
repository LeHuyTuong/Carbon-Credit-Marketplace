package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayoutFormulaResponse {
    private final ProfitSharingProperties.PricingMode pricingMode;
    private final BigDecimal unitPrice;
    private final BigDecimal minPayout;
    private final BigDecimal unitPricePerKwh;
    private final BigDecimal unitPricePerCredit;
    private final String currency;
}

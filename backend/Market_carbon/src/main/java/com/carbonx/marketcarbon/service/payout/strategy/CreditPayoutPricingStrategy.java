package com.carbonx.marketcarbon.service.payout.strategy;

import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Công thức CREDIT (mặc định): kg CO2e -> tấn (credit) -> tiền.
 * Giữ nguyên phép chia 1000, scale 6 và làm tròn HALF_UP của implement gốc.
 */
@Component
public class CreditPayoutPricingStrategy implements PayoutPricingStrategy {

    private static final BigDecimal KG_PER_CREDIT = new BigDecimal("1000");

    @Override
    public PricingMode supportedMode() {
        return PricingMode.CREDIT;
    }

    @Override
    public BigDecimal calculateRawPayout(BigDecimal energyContribution,
                                         BigDecimal creditContributionKg,
                                         BigDecimal actualPayoutPricePerCredit,
                                         BigDecimal kwhPerCreditFactor) {
        BigDecimal creditsAsTco2e = zeroIfNull(creditContributionKg)
                .divide(KG_PER_CREDIT, 6, RoundingMode.HALF_UP);

        return creditsAsTco2e
                .multiply(zeroIfNull(actualPayoutPricePerCredit))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

package com.carbonx.marketcarbon.service.payout.strategy;

import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Công thức KWH: quy đổi giá 1 credit sang giá 1 kWh rồi nhân năng lượng sạc.
 * Khi hệ số quy đổi bằng 0 (hoặc null), fallback sang công thức CREDIT như implement gốc.
 */
@Component
public class KwhPayoutPricingStrategy implements PayoutPricingStrategy {

    private final PayoutPricingStrategy creditPayoutPricingStrategy;

    public KwhPayoutPricingStrategy(
            @Qualifier("creditPayoutPricingStrategy") PayoutPricingStrategy creditPayoutPricingStrategy) {
        this.creditPayoutPricingStrategy = creditPayoutPricingStrategy;
    }

    @Override
    public PricingMode supportedMode() {
        return PricingMode.KWH;
    }

    @Override
    public BigDecimal calculateRawPayout(BigDecimal energyContribution,
                                         BigDecimal creditContributionKg,
                                         BigDecimal actualPayoutPricePerCredit,
                                         BigDecimal kwhPerCreditFactor) {
        if (kwhPerCreditFactor == null || kwhPerCreditFactor.compareTo(BigDecimal.ZERO) == 0) {
            return creditPayoutPricingStrategy.calculateRawPayout(
                    energyContribution, creditContributionKg, actualPayoutPricePerCredit, kwhPerCreditFactor);
        }

        BigDecimal payoutPricePerKwh = CreditPayoutPricingStrategy.zeroIfNull(actualPayoutPricePerCredit)
                .divide(kwhPerCreditFactor, 6, RoundingMode.HALF_UP);

        return CreditPayoutPricingStrategy.zeroIfNull(energyContribution)
                .multiply(payoutPricePerKwh)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

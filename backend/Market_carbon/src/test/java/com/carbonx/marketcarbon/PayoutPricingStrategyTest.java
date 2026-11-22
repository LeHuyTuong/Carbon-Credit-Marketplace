package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;
import com.carbonx.marketcarbon.service.payout.strategy.CreditPayoutPricingStrategy;
import com.carbonx.marketcarbon.service.payout.strategy.KwhPayoutPricingStrategy;
import com.carbonx.marketcarbon.service.payout.strategy.PayoutPricingStrategy;
import com.carbonx.marketcarbon.service.payout.strategy.PayoutPricingStrategyRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutPricingStrategyTest {

    private static final BigDecimal PRICE = new BigDecimal("17.50");
    private static final BigDecimal FACTOR = new BigDecimal("250");
    private static final BigDecimal ENERGY = new BigDecimal("1000");
    private static final BigDecimal CREDIT_KG = new BigDecimal("2000");

    private final CreditPayoutPricingStrategy creditStrategy = new CreditPayoutPricingStrategy();
    private final KwhPayoutPricingStrategy kwhStrategy = new KwhPayoutPricingStrategy(creditStrategy);

    @Test
    void kwhStrategyUsesEnergyConvertedByPricePerKwh() {
        BigDecimal result = kwhStrategy.calculateRawPayout(ENERGY, CREDIT_KG, PRICE, FACTOR);

        // 17.50 / 250 = 0.070000 ; 1000 kWh * 0.07 = 70.00
        assertThat(result).isEqualTo(new BigDecimal("70.00"));
    }

    @Test
    void creditStrategyUsesKgDividedByThousandTimesPrice() {
        BigDecimal result = creditStrategy.calculateRawPayout(ENERGY, CREDIT_KG, PRICE, FACTOR);

        // 2000 kg = 2 tCO2e ; 2 * 17.50 = 35.00 (energy/factor không ảnh hưởng)
        assertThat(result).isEqualTo(new BigDecimal("35.00"));
    }

    @Test
    void kwhStrategyFallsBackToCreditWhenFactorIsZero() {
        BigDecimal result = kwhStrategy.calculateRawPayout(ENERGY, CREDIT_KG, PRICE, BigDecimal.ZERO);

        assertThat(result).isEqualTo(new BigDecimal("35.00"));
    }

    @Test
    void kwhStrategyFallsBackToCreditWhenFactorIsNull() {
        BigDecimal result = kwhStrategy.calculateRawPayout(ENERGY, CREDIT_KG, PRICE, null);

        assertThat(result).isEqualTo(new BigDecimal("35.00"));
    }

    @Test
    void nullInputsProduceZeroWithoutThrowing() {
        assertThat(creditStrategy.calculateRawPayout(null, null, null, null))
                .isEqualTo(BigDecimal.ZERO.setScale(2));
        assertThat(kwhStrategy.calculateRawPayout(null, CREDIT_KG, PRICE, FACTOR))
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(kwhStrategy.calculateRawPayout(ENERGY, CREDIT_KG, null, FACTOR))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void zeroInputsProduceZero() {
        assertThat(creditStrategy.calculateRawPayout(BigDecimal.ZERO, BigDecimal.ZERO, PRICE, FACTOR))
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(kwhStrategy.calculateRawPayout(BigDecimal.ZERO, CREDIT_KG, PRICE, FACTOR))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void registryResolvesEachModeAndDefaultsNullToCredit() {
        PayoutPricingStrategyRegistry registry =
                new PayoutPricingStrategyRegistry(List.of(kwhStrategy, creditStrategy));

        assertThat(registry.resolve(PricingMode.KWH)).isSameAs(kwhStrategy);
        assertThat(registry.resolve(PricingMode.CREDIT)).isSameAs(creditStrategy);
        assertThat(registry.resolve(null)).isSameAs(creditStrategy);
    }

    @Test
    void defaultRegistryMatchesExplicitStrategies() {
        PayoutPricingStrategyRegistry registry = PayoutPricingStrategyRegistry.withDefaults();

        assertThat(registry.resolve(PricingMode.KWH).calculateRawPayout(ENERGY, CREDIT_KG, PRICE, FACTOR))
                .isEqualTo(new BigDecimal("70.00"));
        assertThat(registry.resolve(PricingMode.CREDIT).calculateRawPayout(ENERGY, CREDIT_KG, PRICE, FACTOR))
                .isEqualTo(new BigDecimal("35.00"));
    }
}

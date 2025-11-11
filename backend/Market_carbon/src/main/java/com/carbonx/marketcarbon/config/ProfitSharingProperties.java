package com.carbonx.marketcarbon.config;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Static profit sharing policy resolved from configuration.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "profit-sharing")
@Getter
@Setter
public class ProfitSharingProperties {

    private Policy defaultPolicy = new Policy();
    private Map<Long, Policy> overrides = new HashMap<>();

    public ResolvedPolicy resolveForCompany(Long companyId) {
        if (defaultPolicy == null) {
            throw new IllegalStateException("Default profit sharing policy must be configured");
        }
        Policy override = companyId != null ? overrides.get(companyId) : null;
        BigDecimal unitPricePerKwh = override != null && override.getUnitPricePerKwh() != null
                ? override.getUnitPricePerKwh()
                : defaultPolicy.getUnitPricePerKwh();
        BigDecimal unitPricePerCredit = override != null && override.getUnitPricePerCredit() != null
                ? override.getUnitPricePerCredit()
                : defaultPolicy.getUnitPricePerCredit();
        BigDecimal minPayout = Optional.ofNullable(override)
                .map(Policy::getMinPayout)
                .orElse(defaultPolicy.getMinPayout());

        PricingMode pricingMode;
        BigDecimal effectiveUnitPrice;
        if (unitPricePerKwh != null) {
            pricingMode = PricingMode.KWH;
            effectiveUnitPrice = unitPricePerKwh;
        } else if (unitPricePerCredit != null) {
            pricingMode = PricingMode.CREDIT;
            effectiveUnitPrice = unitPricePerCredit;
        } else {
            throw new IllegalStateException("Profit sharing policy must define either kWh or credit unit price");
        }

        return new ResolvedPolicy(pricingMode,
                unitPricePerKwh,
                unitPricePerCredit,
                minPayout,
                effectiveUnitPrice);
    }

    @Data
    public static class Policy {
        private BigDecimal unitPricePerKwh;
        private BigDecimal unitPricePerCredit;
        private BigDecimal minPayout;
    }

    public enum PricingMode {
        KWH,
        CREDIT
    }

    @Getter
    public static class ResolvedPolicy {
        private final PricingMode pricingMode;
        private final BigDecimal unitPricePerKwh;
        private final BigDecimal unitPricePerCredit;
        private final BigDecimal minPayout;
        private final BigDecimal unitPrice;

        private ResolvedPolicy(PricingMode pricingMode,
                               BigDecimal unitPricePerKwh,
                               BigDecimal unitPricePerCredit,
                               BigDecimal minPayout,
                               BigDecimal unitPrice) {
            this.pricingMode = pricingMode;
            this.unitPricePerKwh = scale(unitPricePerKwh);
            this.unitPricePerCredit = scale(unitPricePerCredit);
            this.minPayout = scale(minPayout);
            this.unitPrice = scale(unitPrice);
        }

        private BigDecimal scale(BigDecimal value) {
            return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
        }
    }
}

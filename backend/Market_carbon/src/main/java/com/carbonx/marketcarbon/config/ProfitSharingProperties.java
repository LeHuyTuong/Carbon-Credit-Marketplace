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
        String source = override != null ? String.format("overrides[%d]", companyId) : "defaultPolicy";

        // 1. Lấy Pricing Mode (KWH hay CREDIT)
        PricingMode pricingMode = Optional.ofNullable(override)
                .map(Policy::getPricingMode) // Ưu tiên override
                .orElse(defaultPolicy.getPricingMode()); // Nếu không có, dùng default

        // 2. Lấy % chia sẻ
        BigDecimal ownerSharePct = Optional.ofNullable(override)
                .map(Policy::getOwnerSharePct) // Ưu tiên override
                .orElse(defaultPolicy.getOwnerSharePct()); // Nếu không có, dùng default

        // 3. Lấy Min Payout
        BigDecimal minPayout = Optional.ofNullable(override)
                .map(Policy::getMinPayout)
                .orElse(defaultPolicy.getMinPayout());

        // 4. Lấy Tiền tệ
        String currency = Optional.ofNullable(override)
                .map(Policy::getCurrency)
                .filter(value -> !value.isBlank())
                .orElse(defaultPolicy.getCurrency());

        if (pricingMode == null) {
            // Lỗi này xảy ra nếu file .properties thiếu 'pricingMode'
            throw new IllegalStateException("Profit sharing policy must define a 'pricingMode' (KWH or CREDIT)");
        }
        if (ownerSharePct == null) {
            // Lỗi này xảy ra nếu file .properties thiếu 'ownerSharePct'
            throw new IllegalStateException("Profit sharing policy must define 'ownerSharePct'");
        }

        return new ResolvedPolicy(
                source,
                pricingMode,
                minPayout,
                ownerSharePct,
                currency);
    }

    @Data
    public static class Policy {
        private PricingMode pricingMode; // KWH hoặc CREDIT
        private BigDecimal minPayout;
        private BigDecimal ownerSharePct; // Ví dụ: 0.4 (cho 40%)
        private String currency = "USD";
    }

    public enum PricingMode {
        KWH,
        CREDIT
    }

    @Getter
    public static class ResolvedPolicy {
        private String source;
        private final PricingMode pricingMode;
        private final BigDecimal minPayout;
        private final String currency;
        private final BigDecimal ownerSharePct;

        private ResolvedPolicy(
                String source,
                PricingMode pricingMode,
                BigDecimal minPayout,
                BigDecimal ownerSharePct,
                String currency) {
            this.source = source;
            this.pricingMode = pricingMode;
            this.minPayout = scale(minPayout, 2); // Tiền tệ 2 chữ số
            this.currency = currency == null ? "USD" : currency.trim().toUpperCase();
            this.ownerSharePct = scale(ownerSharePct, 4); // % 4 chữ số (ví dụ 0.4000)
        }

        private BigDecimal scale(BigDecimal value, int scale) {
            return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
        }
    }
}

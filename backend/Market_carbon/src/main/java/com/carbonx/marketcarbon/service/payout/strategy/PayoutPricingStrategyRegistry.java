package com.carbonx.marketcarbon.service.payout.strategy;

import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Chọn chiến lược tính payout theo {@link PricingMode}.
 * Spring inject toàn bộ {@link PayoutPricingStrategy} bean qua constructor.
 */
@Component
public class PayoutPricingStrategyRegistry {

    private final Map<PricingMode, PayoutPricingStrategy> strategiesByMode;

    public PayoutPricingStrategyRegistry(List<PayoutPricingStrategy> strategies) {
        Map<PricingMode, PayoutPricingStrategy> byMode = new EnumMap<>(PricingMode.class);
        if (strategies != null) {
            for (PayoutPricingStrategy strategy : strategies) {
                if (strategy != null && strategy.supportedMode() != null) {
                    byMode.putIfAbsent(strategy.supportedMode(), strategy);
                }
            }
        }
        this.strategiesByMode = byMode;
    }

    /**
     * @param mode null được coi là CREDIT - chế độ mặc định của hệ thống.
     */
    public PayoutPricingStrategy resolve(PricingMode mode) {
        PricingMode effectiveMode = mode == null ? PricingMode.CREDIT : mode;
        PayoutPricingStrategy strategy = strategiesByMode.get(effectiveMode);
        if (strategy == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        return strategy;
    }

    /** Dùng khi registry không được inject (ví dụ unit test khởi tạo service bằng tay). */
    public static PayoutPricingStrategyRegistry withDefaults() {
        CreditPayoutPricingStrategy creditStrategy = new CreditPayoutPricingStrategy();
        return new PayoutPricingStrategyRegistry(
                List.of(new KwhPayoutPricingStrategy(creditStrategy), creditStrategy));
    }
}

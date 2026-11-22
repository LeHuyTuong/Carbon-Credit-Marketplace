package com.carbonx.marketcarbon.service.payout.strategy;

import com.carbonx.marketcarbon.config.ProfitSharingProperties.PricingMode;

import java.math.BigDecimal;

/**
 * Chiến lược tính số tiền payout thô cho một EV Owner.
 *
 * <p>Mỗi implementation giữ nguyên một công thức tính của {@code ProfitSharingServiceImpl}
 * trước khi tách Strategy; registry chọn implementation theo {@link PricingMode}.</p>
 */
public interface PayoutPricingStrategy {

    PricingMode supportedMode();

    /**
     * @param energyContribution       tổng năng lượng sạc (kWh), có thể null
     * @param creditContributionKg     tổng tín chỉ đóng góp (kg CO2e), có thể null
     * @param actualPayoutPricePerCredit giá 1 credit sau khi nhân tỷ lệ chia sẻ, có thể null
     * @param kwhPerCreditFactor       hệ số quy đổi kWh/credit, có thể null
     * @return số tiền làm tròn 2 chữ số thập phân (HALF_UP); trả ZERO cho input null
     */
    BigDecimal calculateRawPayout(BigDecimal energyContribution,
                                  BigDecimal creditContributionKg,
                                  BigDecimal actualPayoutPricePerCredit,
                                  BigDecimal kwhPerCreditFactor);
}

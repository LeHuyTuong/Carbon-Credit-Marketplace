package com.carbonx.marketcarbon.scheduler;

import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.service.DynamicPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateScheduler {

    private final DynamicPricingService dynamicPricingService;
    private final MarketplaceListingRepository  marketplaceListingRepository;

    @Transactional
//    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh") // 8h sáng giờ VN
    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút để test
    public void updateMarketPrices() {
        log.info("[CRON] Running daily price update job (Redis)...");

        // lấy giá trung bình từ repo
        Double priceAvg = marketplaceListingRepository.getWeightedAveragePrice();

        if (priceAvg == null) {
            log.warn("[CRON] No marketplace listing available → skip update.");
            return;
        }

        BigDecimal newPrice = BigDecimal.valueOf(priceAvg)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // Cập nhật giá vào ram
        dynamicPricingService.updateMarketPrice(newPrice);

        // Cập nhật hệ số
        dynamicPricingService.updateKwhPerCreditFactor(new BigDecimal("2500"));

        log.info("[CRON] Updated Redis MarketPrice={} | KwhFactor={}",
                dynamicPricingService.getMarketPricePerCredit(),
                dynamicPricingService.getKwhPerCreditFactor()
        );
    }
}

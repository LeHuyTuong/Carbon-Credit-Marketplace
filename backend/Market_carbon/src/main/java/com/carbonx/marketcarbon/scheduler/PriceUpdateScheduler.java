package com.carbonx.marketcarbon.scheduler;

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
    private final Random random = new Random();

    @Transactional
//    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh") // 8h sáng giờ VN
    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút để test
    public void updateMarketPrices() {
        log.info("[CRON] Running daily price update job (Redis)...");

        // Giả lập giá dao động từ $100 đến $110
        double randomMarketPrice = 100.0 + (random.nextDouble() * 10.0);
        BigDecimal newPrice = new BigDecimal(String.format(Locale.US, "%.2f", randomMarketPrice));

        // Cập nhật giá vào Redis (thông qua Service)
        dynamicPricingService.updateMarketPrice(newPrice);

        // Cập nhật hệ số (nếu cần)
        dynamicPricingService.updateKwhPerCreditFactor(new BigDecimal("2500"));

        log.info("[CRON] Updated Redis MarketPrice={} | KwhFactor={}",
                dynamicPricingService.getMarketPricePerCredit(),
                dynamicPricingService.getKwhPerCreditFactor()
        );
    }
}

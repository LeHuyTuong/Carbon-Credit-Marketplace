package com.carbonx.marketcarbon.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bean Singleton để lưu trữ giá thị trường động (lưu trữ trong RAM).
 * Được cập nhật bởi Cron Job và được đọc bởi Payout Service.
 */
@Service
@Slf4j
@Getter
public class DynamicPricingService {

    // Sử dụng AtomicReference để đảm bảo an toàn khi nhiều luồng cùng đọc/ghi
    private final AtomicReference<BigDecimal> marketPricePerCredit;
    private final AtomicReference<BigDecimal> kwhPerCreditFactor;

    // Giá trị mặc định khi ứng dụng mới khởi động
    private static final BigDecimal DEFAULT_MARKET_PRICE = new BigDecimal("100.00");
    private static final BigDecimal DEFAULT_KWH_FACTOR = new BigDecimal("2500");

    public DynamicPricingService() {
        this.marketPricePerCredit = new AtomicReference<>(DEFAULT_MARKET_PRICE);
        this.kwhPerCreditFactor = new AtomicReference<>(DEFAULT_KWH_FACTOR);
        log.info("DynamicPricingService (In-Memory) initialized with default price: {}", DEFAULT_MARKET_PRICE);
    }

    // Cron Job sẽ gọi hàm này
    public void updateMarketPrice(BigDecimal newPrice) {
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
            marketPricePerCredit.set(newPrice);
            log.info("In-Memory Market price updated to: {}", newPrice);
        }
    }

    // Cron Job cũng có thể gọi hàm này (mặc dù ít thay đổi)
    public void updateKwhPerCreditFactor(BigDecimal newFactor) {
        if (newFactor != null && newFactor.compareTo(BigDecimal.ZERO) > 0) {
            kwhPerCreditFactor.set(newFactor);
            log.info("In-Memory KWH per Credit factor updated to: {}", newFactor);
        }
    }

    // Service Payout sẽ gọi hàm này
    public BigDecimal getMarketPricePerCredit() {
        return marketPricePerCredit.get();
    }

    // Service Payout sẽ gọi hàm này
    public BigDecimal getKwhPerCreditFactor() {
        return kwhPerCreditFactor.get();
    }
}

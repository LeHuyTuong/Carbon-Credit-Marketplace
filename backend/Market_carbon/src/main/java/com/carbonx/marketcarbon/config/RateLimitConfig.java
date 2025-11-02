package com.carbonx.marketcarbon.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.capacity}")
    private int capacity;

    @Value("${app.rate-limit.refill}")
    private int refill;

    @Value("${app.rate-limit.duration-minutes}")
    private int durationInMinutes;

    // Map này sẽ lưu bucket cho từng KEY (Key = IP + API)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Phương thức này để tìm hoặc tạo mới bucket cho 1 key
    public Bucket resolveBucket(String key) {
        // computeIfAbsent sẽ tự động gọi newBucket(key) nếu key chưa có
        return cache.computeIfAbsent(key, this::newBucket);
    }

    // Phương thức này tạo ra 1 bucket mới với cấu hình đã định
    private Bucket newBucket(String key) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refill, Duration.ofMinutes(durationInMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}

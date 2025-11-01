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
    @Value("${rate-limit.capacity}")
    private int capacity;

    @Value("${rate-limit.refill}")
    private int refill;

    @Value("${rate-limit.duration-minutes}")
    private int durationInMinutes;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>(); // bộ nhớ local của instance Spring Boot.

    // Phương thức này để tìm hoặc tạo mới bucket cho 1 IP (apiKey)
    public Bucket resolveBucket(String apiKey) {
        return cache.computeIfAbsent(apiKey, this::newBucket);
    }

    private Bucket newBucket(String apiKey) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refill, Duration.ofMinutes(durationInMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}

package com.carbonx.marketcarbon.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @GetMapping("/resource")
    public ResponseEntity<String> getResource() {
        String clientId = "client-id"; // có thể lấy từ request
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> {
            Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });

        if (bucket.tryConsume(1)) {
            return ResponseEntity.ok("Data");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded");
        }
    }
}

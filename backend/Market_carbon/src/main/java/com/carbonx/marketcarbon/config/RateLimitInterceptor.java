package com.carbonx.marketcarbon.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Ưu tiên X-Forwarded-For, nếu không có mới dùng getRemoteAddr
        String apiKey = request.getHeader("X-Forwarded-For");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = request.getRemoteAddr();
        } else {
            // Header X-Forwarded-For có thể chứa nhiều IP (client, proxy1, proxy2), lấy cái đầu tiên
            apiKey = apiKey.split(",")[0].trim();
        }

        // Lấy bucket tương ứng với IP này từ config
        Bucket tokenBucket = rateLimitConfig.resolveBucket(apiKey);

        ConsumptionProbe probe = tokenBucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true; // Cho phép request đi tiếp
        } else {
            // Hết token, chặn request
            long waitForRefillSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "You have exhausted your API request quota");
            return false; // Chặn request
        }
    }
}

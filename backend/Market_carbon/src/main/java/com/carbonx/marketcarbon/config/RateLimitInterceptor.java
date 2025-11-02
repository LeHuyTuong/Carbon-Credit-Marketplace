package com.carbonx.marketcarbon.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    // Phụ thuộc vào RateLimitConfig (để quản lý Map)
    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // B1: Lấy IP
        String apiKey = request.getHeader("X-Forwarded-For");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = request.getRemoteAddr();
        } else {
            apiKey = apiKey.split(",")[0].trim();
        }

        // B2: Lấy tên API (Controller + Method)
        String endpointKey;
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            endpointKey = handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        } else {
            endpointKey = request.getRequestURI(); // Fallback
        }

        // B3: Tạo KEY TỔ HỢP (ví dụ: "1.2.3.4-ProjectController#getProjectById")
        String compositeKey = apiKey + "-" + endpointKey;

        log.debug("RateLimit Key: {}", compositeKey);

        // B4: Lấy bucket cho key này
        Bucket tokenBucket = rateLimitConfig.resolveBucket(compositeKey);

        // B5: Kiểm tra token
        ConsumptionProbe probe = tokenBucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true; // OK, cho đi tiếp
        } else {
            // Hết token, chặn
            long waitForRefillSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "You have exhausted your API request quota for this endpoint");
            return false; // Chặn
        }
    }
}

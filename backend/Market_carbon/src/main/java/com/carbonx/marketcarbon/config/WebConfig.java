package com.carbonx.marketcarbon.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor  rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")                  // áp cho toàn bộ API
                .excludePathPatterns(
                        "/api/resource",                     // ngoại lệ
                        "/v3/api-docs/**", "/swagger-ui/**" // tránh throttle swagger
                );
    }
}

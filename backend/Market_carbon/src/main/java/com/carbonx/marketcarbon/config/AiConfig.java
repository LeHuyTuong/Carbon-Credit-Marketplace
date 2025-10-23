// src/main/java/com/carbonx/marketcarbon/config/AiConfig.java
package com.carbonx.marketcarbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.gemini")
public class AiConfig {

    private String apiKey;
    private String model = "gemini-1.5-flash";
    private boolean enabled = true;
    private int timeoutMs = 240000; // 60 giây để tránh timeout sớm

    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {

        // Cấu hình HttpClient với các mức timeout chi tiết
        HttpClient http = HttpClient.create()
                // Timeout khi kết nối TCP
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                // Timeout khi đọc / ghi dữ liệu (stream)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                )
                // Timeout tổng thể cho toàn bộ request
                .responseTimeout(Duration.ofMillis(timeoutMs * 2));

        // Cấu hình WebClient chính
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                // Dễ debug nếu cần: hiển thị thông tin request
                .filter((request, next) -> {
                    System.out.println(" [Gemini Request] " + request.method() + " " + request.url());
                    return next.exchange(request);
                })
                .build();
    }
}

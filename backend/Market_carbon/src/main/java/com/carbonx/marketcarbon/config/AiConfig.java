package com.carbonx.marketcarbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private String model = "gemini-2.5-pro";

    private boolean enabled = true;

    private int timeoutMs = 60000;

    private String apiVersion = "v1";

    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                )
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .filter((request, next) -> {
                    System.out.println("[Gemini Request] " + request.method() + " " + request.url());
                    return next.exchange(request);
                })
                .build();
    }
}

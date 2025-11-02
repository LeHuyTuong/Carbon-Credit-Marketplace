package com.carbonx.marketcarbon.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.gemini")
public class AiConfig {

    // ====== Gemini (API key) ======
    private String apiKey;
    private String model = "gemini-2.5-pro";
    private boolean enabled = true;
    private int timeoutMs = 60000;
    private String apiVersion = "v1";

    // ====== Vertex (OAuth) ======
    // Giữ lại property để có thể dùng nơi khác nếu cần
    @Value("${ai.vertex.enabled:false}")
    private boolean vertexEnabled;

    @Value("${ai.vertex.project-id:}")
    private String vertexProjectId;

    @Value("${ai.vertex.location:us-central1}")
    private String vertexLocation;

    @Value("${ai.vertex.model:gemini-1.5-pro-002}")
    private String vertexModel;

    @Value("${ai.vertex.timeout-ms:60000}")
    private int vertexTimeoutMs;

    // =============================
    // Gemini WebClient (API key)
    // =============================
    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS)))
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .filter(logRequest())
                .build();
    }

    // =============================
    // Filters
    // =============================
    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            System.out.println("[AI Request] " + request.method() + " " + request.url());
            return next.exchange(request);
        };
    }

    private ExchangeFilterFunction authorizationFilter() {
        return (request, next) -> {
            try {
                String token = fetchAccessToken();
                ClientRequest newRequest = ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(token))
                        .build();
                return next.exchange(newRequest);
            } catch (IOException e) {
                return next.exchange(request);
            }
        };
    }

    private String fetchAccessToken() throws IOException {
        GoogleCredentials creds = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        AccessToken t = creds.getAccessToken();
        if (t == null || t.getExpirationTime() == null || t.getExpirationTime().before(new Date())) {
            creds.refreshIfExpired();
            t = creds.getAccessToken();
        }
        if (t == null) throw new IOException("Failed to obtain Google access token (ADC).");
        return t.getTokenValue();
    }
}

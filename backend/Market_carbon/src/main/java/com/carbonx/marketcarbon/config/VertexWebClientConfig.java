package com.carbonx.marketcarbon.config;

import lombok.extern.slf4j.Slf4j;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VertexWebClientConfig {

    private final AiVertexConfig cfg;

    private GoogleCredentials credentials() throws Exception {
        // Đường dẫn file khoá từ cấu hình (GOOGLE_APPLICATION_CREDENTIALS). Trước đây nó bị
        // hardcode thành "carbonx-ai-6060f3108572.json" — file không có trong repo nên prod
        // không boot được (FileNotFoundException).
        String path = cfg.getCredentialsFile();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                    "ai.vertex.credentials-file is not configured (set GOOGLE_APPLICATION_CREDENTIALS)");
        }
        java.io.File keyFile = new java.io.File(path);
        try (java.io.FileInputStream serviceAccountStream = new java.io.FileInputStream(keyFile)) {
            GoogleCredentials creds = GoogleCredentials.fromStream(serviceAccountStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            creds.refreshIfExpired();
            return creds;
        }
    }

    @Bean("vertexWebClient")
    public WebClient vertexWebClient() throws Exception {
        String baseUrl = "https://" + cfg.getLocation() + "-aiplatform.googleapis.com";

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(cfg.getTimeoutMs()));

        // P1.2: when Vertex is disabled (tests, or deployments without AI), build a plain
        // client WITHOUT touching Google auth — application startup must not depend on an
        // external identity provider being reachable/valid. AI calls are gated by
        // cfg.isEnabled() at the service layer and will simply report "disabled".
        if (!cfg.isEnabled()) {
            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(http))
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Accept", "application/json")
                    .build();
        }

        GoogleCredentials creds;
        try {
            creds = credentials();
        } catch (Exception e) {
            // Không có khoá Vertex thì vẫn phải boot được: các tính năng AI sẽ tự báo
            // "disabled" ở tầng service.
            log.error("Vertex AI credentials unavailable ({}); AI features will report disabled", e.getMessage());
            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(http))
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Accept", "application/json")
                    .build();
        }

        //  Filter tự động chèn Bearer Token IAM vào mỗi request
        ExchangeFilterFunction oauth = (request, next) -> {
            try {
                creds.refreshIfExpired();
                AccessToken token = creds.getAccessToken();
                ClientRequest authed = ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(token.getTokenValue()))
                        .build();
                return next.exchange(authed);
            } catch (Exception e) {
                return Mono.error(e);
            }
        };

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .filter(oauth)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @PostConstruct
    public void testVertexAuth() {
        try {
            GoogleCredentials creds = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            creds.refreshIfExpired();
            AccessToken token = creds.getAccessToken();
            log.info("Vertex AI credentials resolved, token expires at {}", token.getExpirationTime());
        } catch (Exception e) {
            log.error("Vertex AI authentication failed at startup; AI features will be unavailable", e);
        }
    }
}

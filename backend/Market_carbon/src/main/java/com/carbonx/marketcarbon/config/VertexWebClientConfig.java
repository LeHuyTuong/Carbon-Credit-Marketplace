package com.carbonx.marketcarbon.config;

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

@Configuration
@RequiredArgsConstructor
public class VertexWebClientConfig {

    private final AiVertexConfig cfg;

    private GoogleCredentials credentials() throws Exception {
        // Yêu cầu biến môi trường: GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa.json
        GoogleCredentials creds = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        creds.refreshIfExpired();
        return creds;
    }

    @Bean("vertexWebClient")
    public WebClient vertexWebClient() throws Exception {
        String baseUrl = "https://" + cfg.getLocation() + "-aiplatform.googleapis.com";

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(cfg.getTimeoutMs()));

        GoogleCredentials creds = credentials();

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
            System.out.println("[VertexAI] Auth OK. Token prefix: "
                    + token.getTokenValue().substring(0, 20) + "...");
        } catch (Exception e) {
            System.err.println("[VertexAI]  Auth failed: " + e.getMessage());
        }
    }
}

package com.carbonx.marketcarbon.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class VertexGeminiService {
    private final String projectId;
    private final String region;
    private final String model;
    private final GoogleCredentials creds;

    public VertexGeminiService(String projectId, String region, String model, String saJsonPath) throws Exception {
        this.projectId = projectId;
        this.region = region;
        this.model = model;
        try (FileInputStream in = new FileInputStream(saJsonPath)) {
            this.creds = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        }
    }

    private String token() throws Exception {
        creds.refreshIfExpired();
        return creds.getAccessToken().getTokenValue();
    }

    public String generateText(String prompt) throws Exception {
        String endpoint = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                region, projectId, region, model
        );

        String body = """
        {"contents":[{"role":"user","parts":[{"text":%s}]}]}
        """.formatted(json(prompt));

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(URI.create(endpoint));
            post.setHeader("Authorization", "Bearer " + token());
            post.setHeader("Content-Type", "application/json; charset=utf-8");
            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            return http.execute(post, resp -> new String(resp.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String json(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}

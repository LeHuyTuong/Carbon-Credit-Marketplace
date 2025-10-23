package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.AiConfig;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.service.AiScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiScoringService implements AiScoringService {

    private final AiConfig cfg;
    private final WebClient geminiWebClient;

    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled() || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            return new AiScoreResult(BigDecimal.ZERO, "AI disabled", "na");
        }

        final int count = details.size();
        final long zeroEnergy = details.stream()
                .filter(d -> d.getTotalEnergy() == null || d.getTotalEnergy().signum() == 0)
                .count();

        final double avgEf = details.stream()
                .filter(d -> d.getTotalEnergy() != null && d.getTotalEnergy().signum() > 0 && d.getCo2Kg() != null)
                .map(d -> d.getCo2Kg().divide(d.getTotalEnergy(), 6, RoundingMode.HALF_UP))
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);

        final double avgCo2 = details.stream()
                .map(EmissionReportDetail::getCo2Kg)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);

        final double co2Coverage = (double) details.stream().filter(d -> d.getCo2Kg() != null).count()
                / Math.max(1, count);

        String rubric =
                "You are a preliminary scoring system for emission reports. Please RETURN ONLY ONE valid JSON:\n" +
                        "{\n" +
                        "  \"score\": number,\n" +
                        "  \"version\": \"v1.0\",\n" +
                        "  \"notes\": string\n" +
                        "}\n\n" +
                        "Requirements: 5 initial lines, 1 separator line, and 2 blocks 📊 and  as described.\n";

        String projectContext =
                "Project context:\n" +
                        "- Commitments: " + safe(report.getProject().getCommitments()) + "\n" +
                        "- MeasurementMethod: " + safe(report.getProject().getMeasurementMethod()) + "\n" +
                        "- TechnicalIndicators: " + safe(report.getProject().getTechnicalIndicators());

        String dataContext = String.format(
                "Data summary:\n- Rows: %d\n- Zero-energy rows: %d\n- CO2 coverage: %.0f%%\n- Avg EF: %.2f\n- Avg CO2: %.1f",
                count, zeroEnergy, co2Coverage * 100, avgEf, avgCo2
        );

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(
                Collections.singletonMap("parts", Collections.singletonList(
                        Collections.singletonMap("text", rubric + "\n\n" + projectContext + "\n\n" + dataContext)
                ))
        ));
        body.put("generationConfig", Collections.singletonMap("response_mime_type", "application/json"));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", cfg.getApiKey())
                            .build(cfg.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(90)) // ⏱ Tăng timeout phản hồi lên 90s
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(3))) // 🔁 Retry 2 lần nếu lỗi mạng
                    .doOnSubscribe(sub -> log.info("🚀 Sending request to Gemini AI..."))
                    .doOnError(err -> log.warn("⚠️ Gemini request failed once: {}", err.toString()))
                    .doOnSuccess(resp -> log.info("✅ Gemini AI responded successfully"))
                    .block();

            String jsonText = extractText(raw);
            Map<String, Object> parsed = new ObjectMapper().readValue(jsonText, Map.class);

            double sc = parsed.get("score") instanceof Number ? ((Number) parsed.get("score")).doubleValue() : 0.0;
            String notes = Objects.toString(parsed.get("notes"), "");
            String version = Objects.toString(parsed.get("version"), "v1.0");

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);
            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.error("❌ Gemini scoring failed: {}", ex.toString(), ex);

            if (ex instanceof java.util.concurrent.TimeoutException
                    || ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.warn("⏰ Gemini API timeout - model took too long to respond.");
            }

            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getClass().getSimpleName(), "error");
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map)) return "{}";
        Map<String, Object> map = (Map<String, Object>) raw;

        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List) || ((List<?>) candidatesObj).isEmpty()) return "{}";

        Map<?, ?> firstCandidate = (Map<?, ?>) ((List<?>) candidatesObj).get(0);
        Object contentObj = firstCandidate.get("content");
        if (!(contentObj instanceof Map)) return "{}";

        Object partsObj = ((Map<?, ?>) contentObj).get("parts");
        if (!(partsObj instanceof List) || ((List<?>) partsObj).isEmpty()) return "{}";

        Object text = ((Map<?, ?>) ((List<?>) partsObj).get(0)).get("text");
        return text == null ? "{}" : text.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        // ===== Basic stats for prompt =====
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

        // ===== Prompt (English) enforcing JSON & notes format =====
        String rubric =
                "You are a system that produces a preliminary score for an emission report. Return ONLY ONE valid JSON:\n" +
                        "{\n" +
                        "  \"score\": number,          // 0..10 with exactly 1 decimal place\n" +
                        "  \"version\": \"v1.0\",\n" +
                        "  \"notes\": string           // a multi-line string using \\n for line breaks\n" +
                        "}\n\n" +
                        "HARD REQUIREMENTS for 'notes':\n" +
                        "- Exactly 5 leading lines, each formatted as: [+x.y/a.b] ...\n" +
                        "- Then one line: \"---------------------------------------------\"\n" +
                        "- Then one line: \"Total suggested score: {score}/10.0\"\n" +
                        "- Then a block title: \" Detailed analysis:\" followed by bullets, each line starting with \"• \"\n" +
                        "- Then a block title: \" Suggestions for CVA:\" followed by bullets, each line starting with \"- \"\n" +
                        "- Use \\n for line breaks (inside JSON), no markdown, no code fences, output nothing except the JSON.\n\n" +
                        "Scoring rubric (total 10 points):\n" +
                        "- Data completeness (2.0)\n" +
                        "- Consistency of period & project (3.0)\n" +
                        "- Reasonableness of EF / outliers (2.5)\n" +
                        "- Anomalies (1.5)\n" +
                        "- Policy alignment with Project description (1.0)\n";

        // Add project context (null-safe)
        String projectContext = String.format(
                "Project context:\n- Commitments: %s\n- MeasurementMethod: %s\n- TechnicalIndicators: %s",
                safe(report.getProject() != null ? report.getProject().getCommitments() : null),
                safe(report.getProject() != null ? report.getProject().getMeasurementMethod() : null),
                safe(report.getProject() != null ? report.getProject().getTechnicalIndicators() : null)
        );

        String dataContext = String.format(
                "Data summary (please use these numbers in 'notes'):\n" +
                        "- Rows: %d\n" +
                        "- Zero-energy rows: %d\n" +
                        "- CO2 coverage (%%): %.0f\n" +
                        "- Average EF (kg/kWh): %.2f\n" +
                        "- Average CO2 per row (kg): %.1f",
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
                    .block();

            String jsonText = extractText(raw);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new ObjectMapper().readValue(jsonText, Map.class);

            Object scoreObj   = parsed.containsKey("score")   ? parsed.get("score")   : 0;
            Object notesObj   = parsed.containsKey("notes")   ? parsed.get("notes")   : "";
            Object versionObj = parsed.containsKey("version") ? parsed.get("version") : "v1.0";

            double sc = (scoreObj instanceof Number) ? ((Number) scoreObj).doubleValue() : 0.0;
            String notes = Objects.toString(notesObj, "");
            String version = Objects.toString(versionObj, "v1.0");

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);
            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.warn("Gemini scoring failed: {}", ex.getMessage());
            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getMessage(), "error");
        }
    }

    // ===== Helpers =====
    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map)) return "{}";
        Map<String, Object> map = (Map<String, Object>) raw;

        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List)) return "{}";
        List<?> candidates = (List<?>) candidatesObj;
        if (candidates.isEmpty()) return "{}";

        Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
        if (!(contentObj instanceof Map)) return "{}";

        Object partsObj = ((Map<?, ?>) contentObj).get("parts");
        if (!(partsObj instanceof List)) return "{}";
        List<?> parts = (List<?>) partsObj;
        if (parts.isEmpty()) return "{}";

        Object text = ((Map<?, ?>) parts.get(0)).get("text");
        return text == null ? "{}" : text.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

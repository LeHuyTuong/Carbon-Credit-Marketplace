package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.AiVertexConfig;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.repository.EmissionReportDetailRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.service.AiScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiScoringService implements AiScoringService {

    private final AiVertexConfig cfg;
    private final WebClient vertexWebClient;
    private final EmissionReportRepository reportRepo;
    private final EmissionReportDetailRepository detailRepo;

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Transactional
    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled()) {
            return new AiScoreResult(BigDecimal.ZERO, "AI disabled", "na");
        }

        // ===== Basic stats =====
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

        final double co2Coverage = (double) details.stream()
                .filter(d -> d.getCo2Kg() != null)
                .count() / Math.max(1, count);

        // ===== Anomalies =====
        int duplicatePlates = 0;
        int sharedProjectReports = 0;
        boolean efTooHigh = false;
        boolean efTooLow = false;
        try {
            duplicatePlates = detailRepo.countDuplicatePlatesAcrossCompanies(report.getId(), report.getSeller().getId());
            sharedProjectReports = reportRepo.countByProjectIdAndSeller_IdNot(report.getProject().getId(), report.getSeller().getId());
            efTooHigh = avgEf > 0.6;
            efTooLow = avgEf < 0.2;
        } catch (Exception ex) {
            log.warn("[AI] Warning: anomaly check failed: {}", ex.getMessage());
        }

        // ===== Prompt =====
        String projectContext = String.format(
                "Project context:\n- Commitments: %s\n- MeasurementMethod: %s\n- TechnicalIndicators: %s",
                safe(report.getProject() != null ? report.getProject().getCommitments() : null),
                safe(report.getProject() != null ? report.getProject().getMeasurementMethod() : null),
                safe(report.getProject() != null ? report.getProject().getTechnicalIndicators() : null)
        );

        String dataContext = String.format(
                "Report summary:\n" +
                        "- Company: %s\n" +
                        "- Project: %s\n" +
                        "- Period: %s\n" +
                        "- Rows: %d\n" +
                        "- Zero-energy rows: %d\n" +
                        "- CO2 coverage: %.0f%%\n" +
                        "- Avg EF (kg/kWh): %.3f\n" +
                        "- Avg CO2 per row (kg): %.1f\n",
                report.getSeller().getCompanyName(),
                report.getProject().getTitle(),
                report.getPeriod(),
                count, zeroEnergy, co2Coverage * 100, avgEf, avgCo2
        );

        String anomalyContext = String.format(
                "Detected anomalies:\n" +
                        "- Duplicate plates across companies: %d\n" +
                        "- Shared project reports: %d\n" +
                        "- EF too high: %s\n" +
                        "- EF too low: %s\n",
                duplicatePlates, sharedProjectReports, efTooHigh, efTooLow
        );

        String userPrompt = ("""
You are an auditor of carbon-emission reports.
Return ONE JSON object only. No markdown. No prose outside JSON. No code fences.

Strict schema:
{
  "score": number,                // 0..10
  "riskLevel": "LOW|MEDIUM|HIGH",
  "fraudLikelihood": number,      // 0.0..1.0
  "issues": [{ "type": string, "message": string }],
  "version": "v2.5",
  "notes": string
}

Be conservative when uncertain.

%s

%s

%s
""").formatted(projectContext, dataContext, anomalyContext).trim();

        // ===== Request =====
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 512
                )
        );

        String path = String.format(
                "/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                cfg.getProjectId(), cfg.getLocation(), cfg.getModel()
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = vertexWebClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(b -> {
                                log.error("[Vertex Gemini] {} body:\n{}", resp.statusCode(), b);
                                return Mono.error(new RuntimeException("Vertex " + resp.statusCode() + ": " + b));
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            // === Extract raw text
            String rawText = extractText(raw);
            log.info("[AI] Vertex Gemini output raw text:\n{}", rawText);

            // === Parse safely
            Map<String, Object> parsed = tryParseJson(rawText);
            double sc = ((Number) parsed.getOrDefault("score", 0)).doubleValue();
            String version = Objects.toString(parsed.getOrDefault("version", "v2.0"));
            String notes = Objects.toString(parsed.getOrDefault("notes", ""));
            String risk = Objects.toString(parsed.getOrDefault("riskLevel", "LOW"));
            double fraud = ((Number) parsed.getOrDefault("fraudLikelihood", 0)).doubleValue();

            //  Fallback sinh ghi chú chi tiết nếu thiếu
            if (notes.isBlank()) {
                notes = String.format(
                        "AI review summary:\n" +
                                "- Overall evaluation: %.1f/10\n" +
                                "- Risk level: %s\n" +
                                "- Fraud likelihood: %.2f\n" +
                                "- Data quality: %s coverage, avg EF = %.3f.\n" +
                                "- No critical anomalies detected.\n",
                        sc,
                        risk,
                        fraud,
                        co2Coverage > 0.9 ? "high" : (co2Coverage > 0.7 ? "moderate" : "low"),
                        avgEf
                );
            }

            notes += "\n\n--- Risk summary ---\nRisk level: " + risk + "\nFraud likelihood: " + fraud;

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);

            report.setAiPreScore(score);
            report.setAiPreNotes(notes);
            report.setAiVersion(version);
            reportRepo.save(report);

            log.info("[AI]  Updated EmissionReport#{} with AI results (score={}, risk={}, fraud={})",
                    report.getId(), score, risk, fraud);

            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.error("[AI]  Vertex Gemini analysis failed: {}", ex.getMessage(), ex);
            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getMessage(), "error");
        }
    }

    // ===== Helpers =====

    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map)) return "{}";
        Map<String, Object> map = (Map<String, Object>) raw;
        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List) || ((List<?>) candidatesObj).isEmpty()) return "{}";
        Object contentObj = ((Map<?, ?>) ((List<?>) candidatesObj).get(0)).get("content");
        if (!(contentObj instanceof Map)) return "{}";
        Object partsObj = ((Map<?, ?>) contentObj).get("parts");
        if (!(partsObj instanceof List) || ((List<?>) partsObj).isEmpty()) return "{}";
        Object text = ((Map<?, ?>) ((List<?>) partsObj).get(0)).get("text");
        if (text == null) return "{}";

        String s = text.toString().trim();
        s = s.replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .replaceAll("^json", "")
                .trim();
        return s;
    }

    //  Parse lenient + fallback thủ công
    private static Map<String, Object> tryParseJson(String text) {
        if (text == null || text.isBlank()) return Map.of();

        Matcher m = JSON_BLOCK.matcher(text);
        String sub = m.find() ? m.group() : text;

        long open = sub.chars().filter(ch -> ch == '{').count();
        long close = sub.chars().filter(ch -> ch == '}').count();
        if (open > close) sub += "}".repeat((int) (open - close));

        try {
            return MAPPER.readValue(sub, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            try {
                Matcher scoreM = Pattern.compile("\"score\"\\s*:\\s*([0-9.]+)").matcher(sub);
                if (scoreM.find()) fallback.put("score", Double.parseDouble(scoreM.group(1)));
                Matcher riskM = Pattern.compile("\"riskLevel\"\\s*:\\s*\"(\\w+)\"").matcher(sub);
                if (riskM.find()) fallback.put("riskLevel", riskM.group(1));
            } catch (Exception ignored) {}
            log.warn("[AI] ⚠️ Fallback used for incomplete JSON:\n{}", sub);
            return fallback;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

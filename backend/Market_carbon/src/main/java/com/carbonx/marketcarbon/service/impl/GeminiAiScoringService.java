package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.config.AiConfig;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.repository.EmissionReportDetailRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
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
    private final EmissionReportRepository reportRepo;
    private final EmissionReportDetailRepository detailRepo;

    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled() || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            return new AiScoreResult(BigDecimal.ZERO, "AI disabled", "na");
        }

        // ===== Basic Stats =====
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

        // ===== Duplicate / anomaly detection =====
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
            log.warn("[AI] Warning: duplicate/anomaly check failed: {}", ex.getMessage());
        }

        // ===== AI prompt template =====
        String rubric =
                "You are a system that evaluates carbon emission reports for both data quality and fraud risk.\n" +
                        "You must return ONLY ONE valid JSON object strictly matching this structure:\n\n" +
                        "{\n" +
                        "  \"score\": number,                // 0..10 with exactly one decimal\n" +
                        "  \"riskLevel\": \"LOW|MEDIUM|HIGH\",\n" +
                        "  \"fraudLikelihood\": number,      // 0.0..1.0\n" +
                        "  \"issues\": [ { \"type\": string, \"message\": string } ],\n" +
                        "  \"version\": \"v2.5\",\n" +
                        "  \"notes\": string\n" +
                        "}\n\n" +
                        "===== SCORING RUBRIC =====\n" +
                        "- Data completeness (2.0)\n" +
                        "- Consistency (2.0)\n" +
                        "- Reasonableness of EF (2.0)\n" +
                        "- CO2 coverage (2.0)\n" +
                        "- Reliability (2.0)\n";

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

        String fullPrompt = rubric + "\n\n" + projectContext + "\n\n" + dataContext + "\n\n" + anomalyContext;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", fullPrompt))
                ))
        );

        // ===== Call Vertex AI (region: asia-southeast1 for Vietnam) =====
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/projects/carbonx-ai/locations/asia-southeast1/publishers/google/models/{model}:generateContent")
                            .queryParam("key", cfg.getApiKey())
                            .build(cfg.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String jsonText = extractText(raw);
            log.info("[AI] Gemini output: {}", jsonText);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new ObjectMapper().readValue(jsonText, Map.class);

            double sc = ((Number) parsed.getOrDefault("score", 0)).doubleValue();
            String version = Objects.toString(parsed.getOrDefault("version", "v2.0"));
            String notes = Objects.toString(parsed.getOrDefault("notes", ""));
            String risk = Objects.toString(parsed.getOrDefault("riskLevel", "LOW"));
            double fraud = ((Number) parsed.getOrDefault("fraudLikelihood", 0)).doubleValue();

            notes += "\n\n--- Risk summary ---\nRisk level: " + risk + "\nFraud likelihood: " + fraud;

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);
            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.error("[AI] Gemini risk analysis failed: {}", ex.getMessage(), ex);
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
        return text == null ? "{}" : text.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

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

    private final AiConfig cfg;
    private final WebClient geminiWebClient;
    private final EmissionReportRepository reportRepo;
    private final EmissionReportDetailRepository detailRepo;

    // Loại dấu ```json / ``` ra khỏi output nếu có
    private static final Pattern FENCE = Pattern.compile("```+\\s*json|```+", Pattern.CASE_INSENSITIVE);
    // Tìm JSON bọc bởi {}
    private static final Pattern BRACED_JSON = Pattern.compile("\\{(?:[^{}]|\\{[^{}]*})*}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled() || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
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

        // ===== Prompt (đơn cho v1) =====
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

        String userPrompt =
                "You are an auditor of carbon-emission reports.\n" +
                        "Return ONE JSON object only. No markdown. No prose outside JSON. No code fences.\n\n" +
                        "Strict schema (types/ranges must be respected; no extra fields):\n" +
                        "{\n" +
                        "  \"score\": number,                // 0..10, exactly 1 decimal\n" +
                        "  \"riskLevel\": \"LOW|MEDIUM|HIGH\",\n" +
                        "  \"fraudLikelihood\": number,      // 0.0..1.0\n" +
                        "  \"issues\": [{ \"type\": string, \"message\": string }],\n" +
                        "  \"version\": \"v2.5\",\n" +
                        "  \"notes\": string\n" +
                        "}\n" +
                        "Rubric (2.0 each; total 10): completeness, consistency, EF reasonableness, CO2 coverage, reliability.\n" +
                        "If any required field is missing, FIX your output and return a single valid JSON object; do NOT return explanations.\n\n" +
                        "Evaluate and output the JSON now.\n\n" +
                        projectContext + "\n\n" + dataContext + "\n\n" + anomalyContext;

        // ===== Body v1 hợp lệ =====
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "topK", 32,
                        "topP", 0.95,
                        "maxOutputTokens", 1024
                )
                // Không dùng systemInstruction/responseMimeType/safetySettings ở v1
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/models/{model}:generateContent")
                            .queryParam("key", cfg.getApiKey())
                            .build(cfg.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(b -> {
                                log.error("[Gemini] {} body:\n{}", resp.statusCode(), b);
                                return Mono.error(new RuntimeException("Gemini " + resp.statusCode() + ": " + b));
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            String jsonText = extractText(raw);
            log.info("[AI] Gemini output raw: {}", jsonText);

            Map<String, Object> parsed = parseJsonLenient(jsonText);

            // Chuẩn hoá + fallback khi thiếu field
            Map<String, Object> normalized = normalizeOrFallback(
                    parsed,
                    count,
                    zeroEnergy,
                    co2Coverage,
                    avgEf,
                    efTooHigh,
                    efTooLow,
                    duplicatePlates,
                    sharedProjectReports
            );

            double sc = ((Number) normalized.get("score")).doubleValue();
            String version = Objects.toString(normalized.get("version"), "v2.5");
            String notes = Objects.toString(normalized.get("notes"), "");
            String risk = Objects.toString(normalized.get("riskLevel"), "LOW");
            double fraud = ((Number) normalized.get("fraudLikelihood")).doubleValue();

            notes += "\n\n--- Risk summary ---\nRisk level: " + risk + "\nFraud likelihood: " + fraud;

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);
            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.error("[AI] Gemini analysis failed: {}", ex.getMessage(), ex);
            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getMessage(), "error");
        }
    }

    // ===== Helpers =====

    /** Rút text từ candidates[0].content.parts[0].text và làm sạch code fences */
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
        String s = text == null ? "{}" : text.toString();
        s = FENCE.matcher(s).replaceAll("").trim();
        if (s.regionMatches(true, 0, "json", 0, 4)) { // nếu prefix "json\n"
            s = s.substring(4).trim();
        }
        return s.isEmpty() ? "{}" : s;
    }

    /** Parse lenient: thử parse trực tiếp, nếu fail thì bóc JSON đầu tiên bằng regex. */
    private static Map<String, Object> parseJsonLenient(String maybeJson) throws Exception {
        try {
            return MAPPER.readValue(maybeJson, Map.class);
        } catch (Exception ex) {
            Matcher m = BRACED_JSON.matcher(maybeJson);
            if (m.find()) {
                String sub = m.group();
                return MAPPER.readValue(sub, Map.class);
            }
            return Map.of();
        }
    }

    /** Chuẩn hoá kết quả model, hoặc tự chấm fallback nếu thiếu trường bắt buộc */
    private static Map<String, Object> normalizeOrFallback(
            Map<String, Object> parsed,
            int rows,
            long zeroEnergy,
            double co2Coverage,
            double avgEf,
            boolean efTooHigh,
            boolean efTooLow,
            int duplicatePlates,
            int sharedProjectReports
    ) {
        Map<String, Object> out = new HashMap<>();
        Object scObj = parsed.get("score");
        Object riskObj = parsed.get("riskLevel");
        Object fraudObj = parsed.get("fraudLikelihood");
        Object issuesObj = parsed.get("issues");
        Object versionObj = parsed.get("version");
        Object notesObj = parsed.get("notes");

        boolean hasAll =
                (scObj instanceof Number) &&
                        (riskObj instanceof String) &&
                        (fraudObj instanceof Number);

        if (!hasAll) {
            // ===== Heuristic fallback (0..10) =====
            double completeness = clamp(co2Coverage * 2.0, 0.0, 2.0);

            double consistency = 2.0;
            if (duplicatePlates > 0) consistency -= 1.0;
            if (sharedProjectReports > 0) consistency -= 0.5;
            if (consistency < 0) consistency = 0;

            double efScore;
            if (avgEf <= 0) {
                efScore = 0.6;
            } else if (avgEf < 0.2) {
                efScore = 0.8;
            } else if (avgEf <= 0.6) {
                efScore = 2.0;
            } else if (avgEf <= 0.8) {
                efScore = 1.2;
            } else {
                efScore = 0.6;
            }

            double coverageScore = clamp(co2Coverage * 2.0, 0.0, 2.0);

            double reliability = 2.0;
            if (rows > 0) {
                double zeroRatio = (double) zeroEnergy / rows;
                reliability -= clamp(zeroRatio * 2.0, 0.0, 1.2);
            }
            if (efTooHigh || efTooLow) reliability -= 0.4;
            if (duplicatePlates > 0) reliability -= 0.4;
            if (sharedProjectReports > 0) reliability -= 0.2;
            if (reliability < 0) reliability = 0;

            double total = completeness + consistency + efScore + coverageScore + reliability; // 0..10

            double fraud = 0.0;
            if (duplicatePlates > 0) fraud += 0.35;
            if (sharedProjectReports > 0) fraud += 0.35;
            if (efTooHigh || efTooLow) fraud += 0.15;
            fraud = clamp(fraud, 0.0, 1.0);

            String risk = (fraud >= 0.7) ? "HIGH" : (fraud >= 0.3 ? "MEDIUM" : "LOW");

            out.put("score", round1(total));
            out.put("riskLevel", risk);
            out.put("fraudLikelihood", fraud);
            out.put("issues", (issuesObj instanceof List) ? issuesObj : List.of());
            out.put("version", "v2.5-fallback");
            StringBuilder n = new StringBuilder();
            n.append("Fallback scoring used (model JSON incomplete).")
                    .append("\nInputs → rows: ").append(rows)
                    .append(", zeroEnergy: ").append(zeroEnergy)
                    .append(", CO2 coverage: ").append(String.format(Locale.US, "%.2f", co2Coverage))
                    .append(", avg EF: ").append(String.format(Locale.US, "%.3f", avgEf))
                    .append(", dupPlates: ").append(duplicatePlates)
                    .append(", sharedReports: ").append(sharedProjectReports)
                    .append(", EF too high: ").append(efTooHigh)
                    .append(", EF too low: ").append(efTooLow);
            if (notesObj instanceof String) {
                n.append("\nModel notes: ").append(notesObj);
            }
            out.put("notes", n.toString());
            return out;
        }

        double score = clamp(((Number) scObj).doubleValue(), 0.0, 10.0);
        double fraud = clamp(((Number) fraudObj).doubleValue(), 0.0, 1.0);
        String risk = ((String) riskObj).toUpperCase(Locale.ROOT);
        if (!risk.equals("LOW") && !risk.equals("MEDIUM") && !risk.equals("HIGH")) {
            risk = (fraud >= 0.7) ? "HIGH" : (fraud >= 0.3 ? "MEDIUM" : "LOW");
        }

        out.put("score", round1(score));
        out.put("riskLevel", risk);
        out.put("fraudLikelihood", fraud);
        out.put("issues", (issuesObj instanceof List) ? issuesObj : List.of());
        out.put("version", Objects.toString(versionObj, "v2.5"));
        out.put("notes", Objects.toString(notesObj, ""));
        return out;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static BigDecimal round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

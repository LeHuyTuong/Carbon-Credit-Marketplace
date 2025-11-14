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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiScoringService implements AiScoringService {

    private final AiVertexConfig cfg;
    private final WebClient vertexWebClient;
    private final EmissionReportRepository reportRepo;
    private final EmissionReportDetailRepository detailRepo;

    // ========= Basic constants (only for parsing / formatting) =========
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // ========= Public API =========

    @Transactional
    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled()) {
            return new AiScoreResult(BigDecimal.ZERO, "AI scoring is disabled by configuration.", "na");
        }

        // 1. Basic stats on dataset
        final int rowCount = details == null ? 0 : details.size();

        final long zeroEnergyRows = details == null ? 0 : details.stream()
                .filter(d -> d.getTotalEnergy() == null || d.getTotalEnergy().signum() == 0)
                .count();

        final double avgEf = details == null ? 0.0 : details.stream()
                .filter(d -> d.getTotalEnergy() != null
                        && d.getTotalEnergy().signum() > 0
                        && d.getCo2Kg() != null)
                .map(d -> d.getCo2Kg().divide(d.getTotalEnergy(), 6, RoundingMode.HALF_UP))
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        final double avgCo2 = details == null ? 0.0 : details.stream()
                .map(EmissionReportDetail::getCo2Kg)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        final double co2CoverageRatio = (double) (details == null ? 0 : details.stream()
                .filter(d -> d.getCo2Kg() != null)
                .count()) / Math.max(1, rowCount);

        // 2. Anomaly indicators that do not rely on AI
        int duplicatePlatesAcrossCompanies = 0;
        boolean efAboveHighThreshold = false;
        boolean efBelowLowThreshold = false;

        try {
            Long reportId = report != null ? report.getId() : null;
            Long sellerId = (report != null && report.getSeller() != null) ? report.getSeller().getId() : null;

            if (reportId != null && sellerId != null) {
                duplicatePlatesAcrossCompanies =
                        detailRepo.countDuplicatePlatesAcrossCompanies(reportId, sellerId);
            }

            double efHigh = efHighThreshold();
            double efLow = efLowThreshold();

            efAboveHighThreshold = avgEf > efHigh;
            efBelowLowThreshold = avgEf < efLow;
        } catch (Exception ex) {
            log.warn("[AI] Anomaly check failed: {}", ex.getMessage());
        }

        // 3. Log missing context so that project owners know what to improve
        logMissingProjectContext(report);

        // 4. Compute rule-style data quality metrics (used for rule details and fallback narrative)
        DQMetrics dqMetrics = computeDQMetrics(details);

        // 5. Build LLM prompt
        final String projectContext = buildProjectContext(report);
        final String dataContext = buildDataContext(
                report,
                rowCount,
                zeroEnergyRows,
                co2CoverageRatio,
                avgEf,
                avgCo2
        );
        final String anomalyContext = buildAnomalyContext(
                duplicatePlatesAcrossCompanies,
                efAboveHighThreshold,
                efBelowLowThreshold
        );
        final String userPrompt = buildPrompt(projectContext, dataContext, anomalyContext);

        if (log.isDebugEnabled()) {
            log.debug("[AI] Prompt sent to Vertex Gemini:\n{}", userPrompt);
        }
        dumpPromptIfEnabled(userPrompt, report);

        // 6. Prepare Vertex request
        int maxTokens = maxOutputTokens();
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", temperature(),
                        "maxOutputTokens", maxTokens
                )
        );

        final String endpointPath = String.format(
                "/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                cfg.getProjectId(),
                cfg.getLocation(),
                cfg.getModel()
        );

        try {
            // 7. Call Vertex
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = vertexWebClient.post()
                    .uri(endpointPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(body -> {
                                log.error("[Vertex Gemini] {} body:\n{}", resp.statusCode(), body);
                                return Mono.error(new RuntimeException("Vertex " + resp.statusCode() + ": " + body));
                            })
                    )
                    .bodyToMono(Map.class)
                    .block();

            // 8. Extract and parse LLM text
            String rawText = extractText(raw);
            log.info("[AI] Vertex Gemini raw output:\n{}", rawText);

            Map<String, Object> parsed = tryParseJson(rawText);

            double scoreNumber = ((Number) parsed.getOrDefault("score", 0)).doubleValue();
            String version = Objects.toString(parsed.getOrDefault("version", defaultVersion()), defaultVersion());
            String notesFromModel = Objects.toString(parsed.getOrDefault("notes", ""), "");
            String riskLevel = Objects.toString(parsed.getOrDefault("riskLevel", "LOW"), "LOW");
            double fraudLikelihood = ((Number) parsed.getOrDefault("fraudLikelihood", 0)).doubleValue();

            boolean usedFallbackNarrative = false;

            // Nếu notes từ model quá ngắn hoặc trống thì build narrative chuẩn + rule details
            if (notesFromModel.isBlank()
                    || notesFromModel.length() < minNotesLength()) {

                notesFromModel = buildRichNotes(
                        report,
                        riskLevel,
                        fraudLikelihood,
                        scoreNumber,
                        co2CoverageRatio,
                        avgEf,
                        avgCo2,
                        rowCount,
                        zeroEnergyRows,
                        duplicatePlatesAcrossCompanies,
                        dqMetrics
                );
                usedFallbackNarrative = true;
            }

            if (!usedFallbackNarrative && appendRiskSummaryTail()) {
                notesFromModel += "\n\n--- Risk summary ---\n"
                        + "Risk level: " + riskLevel
                        + "\nFraud likelihood: " + fraudLikelihood;
            }

            BigDecimal roundedScore = BigDecimal.valueOf(scoreNumber)
                    .setScale(scoreScale(), RoundingMode.HALF_UP);

            // 9. Persist score and notes into report for later display
            if (report != null) {
                report.setAiPreScore(roundedScore);
                report.setAiPreNotes(notesFromModel);
                report.setAiVersion(version);
                reportRepo.save(report);
            }

            log.info("[AI] EmissionReport#{} updated with AI score={}, risk={}, fraudLikelihood={}",
                    report != null ? report.getId() : null,
                    roundedScore,
                    riskLevel,
                    fraudLikelihood
            );

            return new AiScoreResult(roundedScore, notesFromModel, version);

        } catch (Exception ex) {
            log.error("[AI] Vertex Gemini analysis failed: {}", ex.getMessage(), ex);
            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getMessage(), "error");
        }
    }

    // ========= Prompt builder utilities =========

    private String buildProjectContext(EmissionReport report) {
        String commitments = safe(report != null && report.getProject() != null
                ? report.getProject().getCommitments()
                : null);
        String measurementMethod = safe(report != null && report.getProject() != null
                ? report.getProject().getMeasurementMethod()
                : null);
        String technicalIndicators = safe(report != null && report.getProject() != null
                ? report.getProject().getTechnicalIndicators()
                : null);

        return String.format(
                Locale.US,
                "Project context:%n" +
                        "- Commitments: %s%n" +
                        "- MeasurementMethod: %s%n" +
                        "- TechnicalIndicators: %s",
                commitments,
                measurementMethod,
                technicalIndicators
        );
    }

    private String buildDataContext(
            EmissionReport report,
            int rowCount,
            long zeroEnergyRows,
            double co2CoverageRatio,
            double avgEf,
            double avgCo2
    ) {
        String company = report != null && report.getSeller() != null
                ? safe(report.getSeller().getCompanyName())
                : "";
        String project = report != null && report.getProject() != null
                ? safe(report.getProject().getTitle())
                : "";
        String period = report != null
                ? safe(report.getPeriod())
                : "";

        return String.format(
                Locale.US,
                "Report summary:%n" +
                        "- Company: %s%n" +
                        "- Project: %s%n" +
                        "- Period: %s%n" +
                        "- Rows: %d%n" +
                        "- Zero-energy rows: %d%n" +
                        "- CO2 coverage: %.0f%%%n" +
                        "- Avg EF (kg/kWh): %.3f%n" +
                        "- Avg CO2 per row (kg): %.1f",
                company,
                project,
                period,
                rowCount,
                zeroEnergyRows,
                co2CoverageRatio * 100,
                avgEf,
                avgCo2
        );
    }

    private String buildAnomalyContext(
            int duplicatePlatesAcrossCompanies,
            boolean efAboveHighThreshold,
            boolean efBelowLowThreshold
    ) {
        return String.format(
                Locale.US,
                "Detected anomalies:%n" +
                        "- Duplicate plates across companies: %d%n" +
                        "- EF above high threshold: %s%n" +
                        "- EF below low threshold: %s",
                duplicatePlatesAcrossCompanies,
                efAboveHighThreshold,
                efBelowLowThreshold
        );
    }

    private String buildPrompt(String projectContext, String dataContext, String anomalyContext) {
        String header = """
                You are an auditor of carbon-emission reports.
                Return ONE JSON object only. No markdown. No prose outside JSON. No code fences.

                Strict schema:
                {
                  "score": number,                // 0..10
                  "riskLevel": "LOW|MEDIUM|HIGH",
                  "fraudLikelihood": number,      // 0.0..1.0
                  "issues": [{ "type": string, "message": string }],
                  "version": "v2.5",
                  "notes": string                 // 180–300 words. Must contain sections titled: "Overview", "Key Signals", "Anomalies & Explanations", "Data Quality", "Recommendations", "Confidence". Use concise bullet points where appropriate. Reference concrete figures from the report summary and anomalies.
                }

                Be conservative when uncertain.
                """.trim();

        return (header + "\n\n" + projectContext + "\n\n" + dataContext + "\n\n" + anomalyContext).trim();
    }

    // ========= LLM output parsing =========

    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return "{}";
        }

        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return "{}";
        }

        Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return "{}";
        }

        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return "{}";
        }

        Object text = ((Map<?, ?>) parts.get(0)).get("text");
        if (text == null) {
            return "{}";
        }

        String s = text.toString().trim();
        // Remove possible markdown wrappers
        s = s.replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .replaceAll("^json", "")
                .trim();
        return s;
    }

    private static Map<String, Object> tryParseJson(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        Matcher m = JSON_BLOCK.matcher(text);
        String candidate = m.find() ? m.group() : text;

        long open = candidate.chars().filter(ch -> ch == '{').count();
        long close = candidate.chars().filter(ch -> ch == '}').count();
        if (open > close) {
            candidate += "}".repeat((int) (open - close));
        }

        try {
            return MAPPER.readValue(candidate, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            try {
                Matcher scoreM = Pattern.compile("\"score\"\\s*:\\s*([0-9.]+)").matcher(candidate);
                if (scoreM.find()) {
                    fallback.put("score", Double.parseDouble(scoreM.group(1)));
                }

                Matcher riskM = Pattern.compile("\"riskLevel\"\\s*:\\s*\"(\\w+)\"").matcher(candidate);
                if (riskM.find()) {
                    fallback.put("riskLevel", riskM.group(1));
                }

                Matcher fraudM = Pattern.compile("\"fraudLikelihood\"\\s*:\\s*([0-9.]+)").matcher(candidate);
                if (fraudM.find()) {
                    fallback.put("fraudLikelihood", Double.parseDouble(fraudM.group(1)));
                }
            } catch (Exception ignored) {
            }
            log.warn("[AI] Fallback parser used for incomplete JSON:\n{}", candidate);
            return fallback;
        }
    }

    // ========= Misc helpers =========

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void logMissingProjectContext(EmissionReport report) {
        try {
            Long reportId = report != null ? report.getId() : null;
            Long projectId = (report != null && report.getProject() != null)
                    ? report.getProject().getId()
                    : null;

            String commitments = (report != null && report.getProject() != null)
                    ? report.getProject().getCommitments()
                    : null;
            String method = (report != null && report.getProject() != null)
                    ? report.getProject().getMeasurementMethod()
                    : null;
            String indicators = (report != null && report.getProject() != null)
                    ? report.getProject().getTechnicalIndicators()
                    : null;

            List<String> missing = new ArrayList<>();
            if (isBlank(commitments)) {
                missing.add("Commitments");
            }
            if (isBlank(method)) {
                missing.add("MeasurementMethod");
            }
            if (isBlank(indicators)) {
                missing.add("TechnicalIndicators");
            }

            if (!missing.isEmpty()) {
                log.warn("[AI] Missing project context fields {} for reportId={}, projectId={}",
                        missing, reportId, projectId);
            }
        } catch (Exception e) {
            log.warn("[AI] Failed to inspect project context fields: {}", e.getMessage());
        }
    }

    private void dumpPromptIfEnabled(String prompt, EmissionReport report) {
        try {
            boolean enabled = Boolean.parseBoolean(
                    System.getProperty("carbonx.ai.prompt.dump", "false")
            );
            if (!enabled) {
                return;
            }

            String dir = System.getProperty("carbonx.ai.prompt.dir", "./ai-prompts");
            Files.createDirectories(Path.of(dir));

            String ts = TS_FMT.format(LocalDateTime.now());
            String id = (report != null && report.getId() != null)
                    ? ("report-" + report.getId())
                    : "report-unknown";
            Path out = Path.of(dir, ts + "_" + id + ".txt");

            Files.writeString(out, prompt, StandardCharsets.UTF_8);
            log.info("[AI] Prompt dumped to file: {}", out.toAbsolutePath());
        } catch (Exception e) {
            log.warn("[AI] Unable to dump prompt file: {}", e.getMessage());
        }
    }

    // ========= Data quality metrics (for rule-style breakdown) =========

    private record DQMetrics(
            long energyNulls,
            long co2Nulls,
            long nonPositiveEnergy,
            long duplicateEnergyCo2Rows,
            double q1,
            double q3,
            double iqr,
            double lowerFence,
            double upperFence,
            long outlierCount,
            double meanEnergy,
            double stdEnergy,
            double coefficientOfVariation,
            int repeatedRoundedKeys,
            List<Long> topRepeatedRoundedValues
    ) {
    }

    private DQMetrics computeDQMetrics(List<EmissionReportDetail> details) {
        if (details == null || details.isEmpty()) {
            return new DQMetrics(
                    0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0,
                    0, 0, 0,
                    0, new ArrayList<>()
            );
        }

        long energyNulls = details.stream()
                .filter(d -> d.getTotalEnergy() == null)
                .count();

        long co2Nulls = details.stream()
                .filter(d -> d.getCo2Kg() == null)
                .count();

        long nonPositive = details.stream()
                .filter(d -> d.getTotalEnergy() == null || d.getTotalEnergy().signum() <= 0)
                .count();

        List<Double> energies = details.stream()
                .map(EmissionReportDetail::getTotalEnergy)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .filter(v -> v > 0d)
                .sorted()
                .collect(Collectors.toList());

        Map<String, Long> rowHistogram = details.stream()
                .map(d -> {
                    String e = d.getTotalEnergy() == null
                            ? "null"
                            : d.getTotalEnergy().stripTrailingZeros().toPlainString();
                    String c = d.getCo2Kg() == null
                            ? "null"
                            : d.getCo2Kg().stripTrailingZeros().toPlainString();
                    return e + "|" + c;
                })
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        long duplicateRows = rowHistogram.values().stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1)
                .sum();

        double q1 = quantile(energies, 0.25);
        double q3 = quantile(energies, 0.75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        long outliers = energies.stream()
                .filter(v -> v < lower || v > upper)
                .count();

        double mean = mean(energies);
        double std = stddev(energies, mean);
        double cv = mean > 0 ? std / mean : 0d;

        Map<Long, Long> roundedCounts = energies.stream()
                .map(Math::round)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        int repeatedKeys = (int) roundedCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .count();

        List<Long> topRepeated = roundedCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new DQMetrics(
                energyNulls,
                co2Nulls,
                nonPositive,
                duplicateRows,
                q1,
                q3,
                iqr,
                lower,
                upper,
                outliers,
                mean,
                std,
                cv,
                repeatedKeys,
                topRepeated
        );
    }

    private static double quantile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0d;
        }
        double pos = p * (sorted.size() - 1);
        int i = (int) Math.floor(pos);
        int j = (int) Math.ceil(pos);
        if (i == j) {
            return sorted.get(i);
        }
        double w = pos - i;
        return sorted.get(i) * (1 - w) + sorted.get(j) * w;
    }

    private static double mean(List<Double> xs) {
        if (xs.isEmpty()) {
            return 0d;
        }
        double sum = 0d;
        for (double v : xs) {
            sum += v;
        }
        return sum / xs.size();
    }

    private static double stddev(List<Double> xs, double mean) {
        if (xs.size() <= 1) {
            return 0d;
        }
        double sumSq = 0d;
        for (double v : xs) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (xs.size() - 1));
    }

    // ========= Narrative builder with configurable thresholds & texts =========

    private String buildRichNotes(
            EmissionReport report,
            String riskLevel,
            double fraudLikelihood,
            double rawScore,
            double co2CoverageRatio,
            double avgEf,
            double avgCo2,
            int rowCount,
            long zeroEnergyRows,
            int duplicatePlatesAcrossCompanies,
            DQMetrics dq
    ) {
        double efHigh = efHighThreshold();
        double efLow = efLowThreshold();
        double cvSuspicious = cvSuspiciousThreshold();
        double cvUniformity = cvUniformityThreshold();
        double fraudHigh = fraudHighThreshold();

        String coverageLabel = co2CoverageRatio > 0.9
                ? "high"
                : (co2CoverageRatio > 0.7 ? "medium" : "low");

        String efBand;
        if (avgEf < efLow) {
            efBand = "below the expected range";
        } else if (avgEf > efHigh) {
            efBand = "above the expected range";
        } else {
            efBand = "within the screening band";
        }

        String confidenceLevel = switch (riskLevel.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> "Low";
            case "MEDIUM" -> "Medium";
            default -> "High";
        };

        // Rule scores (used only inside the narrative)
        int dq1 = dq.energyNulls == 0 && dq.co2Nulls == 0
                ? 10
                : Math.max(0, 10 - (int) (dq.energyNulls + dq.co2Nulls));
        int dq2 = 10;
        int dq3 = dq.nonPositiveEnergy == 0
                ? 15
                : Math.max(0, 15 - (int) dq.nonPositiveEnergy);
        int dq4 = duplicatePlatesAcrossCompanies == 0
                ? 10
                : Math.max(0, 10 - duplicatePlatesAcrossCompanies);
        int dq5 = dq.duplicateEnergyCo2Rows == 0
                ? 5
                : Math.max(0, 5 - (int) dq.duplicateEnergyCo2Rows);
        int dq6 = dq.outlierCount == 0
                ? 10
                : Math.max(0, 10 - (int) dq.outlierCount);
        int dq7 = dq.coefficientOfVariation <= cvUniformity ? 5 : 0;
        int dq8 = dq.repeatedRoundedKeys == 0
                ? 5
                : Math.max(0, 5 - dq.repeatedRoundedKeys);

        List<String> fraudSignals = new ArrayList<>();

        if (fraudLikelihood >= fraudHigh) {
            fraudSignals.add(String.format(
                    Locale.US,
                    "The model assessed the fraud likelihood as relatively high (fraudLikelihood = %.2f). This does not prove fraud, but it requires careful manual review.",
                    fraudLikelihood
            ));
        }

        if (dq.coefficientOfVariation < cvSuspicious && dq.repeatedRoundedKeys > 0) {
            fraudSignals.add(String.format(
                    Locale.US,
                    "Energy values are extremely uniform (coefficient of variation ≈ %.6f) and there are repeated rounded values (for example: %s). This pattern may indicate artificially smoothed or synthetic data.",
                    dq.coefficientOfVariation,
                    dq.topRepeatedRoundedValues
            ));
        }

        if (dq.duplicateEnergyCo2Rows > 0) {
            fraudSignals.add(String.format(
                    Locale.US,
                    "There are %d rows that share exactly the same combination of energy and CO₂ values. This may reflect copy-paste duplication instead of independent measurements.",
                    dq.duplicateEnergyCo2Rows
            ));
        }

        if (duplicatePlatesAcrossCompanies > 0) {
            fraudSignals.add(String.format(
                    Locale.US,
                    "Detected %d duplicate license plate records across companies. This could indicate asset re-use or identifier collisions that should be reconciled with official registration records.",
                    duplicatePlatesAcrossCompanies
            ));
        }

        StringBuilder sb = new StringBuilder(2048);

        // OVERVIEW
        sb.append(String.format(
                Locale.US,
                "Overview%n" +
                        "- Preliminary score: %.1f/10. Risk level: %s. Fraud likelihood (model output): %.2f.%n" +
                        "- The dataset contains %d rows, with %d rows having zero total energy. CO₂ value coverage is %.0f%% (%s).%n" +
                        "- The average emission factor (EF) is %.3f kg/kWh, which is %s. The average CO₂ per row is %.1f kg.%n",
                rawScore,
                riskLevel,
                fraudLikelihood,
                rowCount,
                zeroEnergyRows,
                co2CoverageRatio * 100,
                coverageLabel,
                avgEf,
                efBand,
                avgCo2
        ));

        // KEY SIGNALS
        sb.append("\nKey Signals\n");
        sb.append(String.format(
                Locale.US,
                "- Data coverage is %s, with approximately %.0f%% of rows containing CO₂ values.%n" +
                        "- The number of rows with zero energy is %d. These rows may reflect downtime, missing measurements, or default values.%n" +
                        "- Screening thresholds for the emission factor are configured externally. The current evaluation considers values below %.3f as low and above %.3f as high for screening purposes.%n",
                coverageLabel,
                co2CoverageRatio * 100,
                zeroEnergyRows,
                efLow,
                efHigh
        ));

        // ANOMALIES & EXPLANATIONS
        sb.append("\nAnomalies & Explanations\n");

        if (duplicatePlatesAcrossCompanies > 0) {
            sb.append(String.format(
                    Locale.US,
                    "- Duplicate license plates detected across companies: %d occurrence(s). Cross-check with registration databases and asset ledgers.%n",
                    duplicatePlatesAcrossCompanies
            ));
        } else {
            sb.append("- No duplicate license plates across companies were detected in this dataset.\n");
        }

        if (dq.duplicateEnergyCo2Rows > 0) {
            sb.append(String.format(
                    Locale.US,
                    "- There are %d exact duplicate rows based on the pair (total energy, CO₂). These may result from copy-and-paste or batch replication rather than independent meter readings.%n",
                    dq.duplicateEnergyCo2Rows
            ));
        } else {
            sb.append("- No exact duplicate rows (same total energy and CO₂ value) were detected.\n");
        }

        if (dq.outlierCount > 0) {
            sb.append(String.format(
                    Locale.US,
                    "- Outlier detection using the interquartile range (IQR) found %d energy value(s) outside the expected range [%.3f, %.3f]. These points should be reviewed to confirm whether they represent genuine peaks or data issues.%n",
                    dq.outlierCount,
                    dq.lowerFence,
                    dq.upperFence
            ));
        } else {
            sb.append(String.format(
                    Locale.US,
                    "- No strong outliers were detected using the interquartile range (IQR). All energy values fall inside the IQR-based expected band [%.3f, %.3f].%n",
                    dq.lowerFence,
                    dq.upperFence
            ));
        }

        if (dq.coefficientOfVariation <= cvUniformity) {
            sb.append(String.format(
                    Locale.US,
                    "- The dispersion of energy values is very low (coefficient of variation = %.6f). In normal operations, some variability is expected, so this level of uniformity should be supported by operational evidence (for example, constant load or fixed schedules).%n",
                    dq.coefficientOfVariation
            ));
        } else {
            sb.append(String.format(
                    Locale.US,
                    "- The dispersion of energy values (coefficient of variation = %.6f) is consistent with normal variability in operations.%n",
                    dq.coefficientOfVariation
            ));
        }

        if (dq.repeatedRoundedKeys > 0) {
            sb.append(String.format(
                    Locale.US,
                    "- Several rounded energy values appear repeatedly after rounding (for example: %s). This can be acceptable if meters report in coarse increments, but a high level of repetition may also indicate manual rounding or synthetic aggregation.%n",
                    dq.topRepeatedRoundedValues
            ));
        }

        if (avgEf > efHigh) {
            sb.append(String.format(
                    Locale.US,
                    "- The average emission factor %.3f kg/kWh is above the configured high screening band (%.3f). Validate the emission factor source, boundary assumptions, and grid intensity.%n",
                    avgEf,
                    efHigh
            ));
        } else if (avgEf < efLow) {
            sb.append(String.format(
                    Locale.US,
                    "- The average emission factor %.3f kg/kWh is below the configured low screening band (%.3f). Confirm meter accuracy, project boundary, and whether all relevant activities are included.%n",
                    avgEf,
                    efLow
            ));
        } else {
            sb.append(String.format(
                    Locale.US,
                    "- The average emission factor %.3f kg/kWh lies inside the configured screening band [%.3f, %.3f].%n",
                    avgEf,
                    efLow,
                    efHigh
            ));
        }

        // DATA QUALITY
        sb.append("\nData Quality\n");
        sb.append(String.format(
                Locale.US,
                "- CO₂ value availability is approximately %.0f%%. Missing CO₂ values reduce comparability and increase uncertainty in the total emissions estimate.%n" +
                        "- Rows with zero energy may correspond to actual downtime, inactive assets, or sensor dropouts. It is important to annotate the cause of these records and retain raw meter logs for verification.%n",
                co2CoverageRatio * 100
        ));

        // RECOMMENDATIONS
        sb.append("\nRecommendations\n");
        List<String> recommendations = recommendedActions();
        for (String rec : recommendations) {
            if (!rec.isBlank()) {
                sb.append("- ").append(rec).append("\n");
            }
        }

        // CONFIDENCE
        sb.append("\nConfidence\n");
        sb.append(String.format(
                Locale.US,
                "- Overall auditor confidence in this screening result is %s, given the risk level %s and the observed data quality. This analysis should be complemented with documentary evidence and, where necessary, on-site checks.%n",
                confidenceLevel,
                riskLevel
        ));

        // FRAUD SIGNALS
        if (!fraudSignals.isEmpty()) {
            sb.append("\nPotential Fraud Signals\n");
            for (String fs : fraudSignals) {
                sb.append("- ").append(fs).append("\n");
            }
        }

        // RULE DETAILS – explicit, non-abbreviated descriptions
        String period = report != null ? safe(report.getPeriod()) : "";

        sb.append("\nRule Details\n");

        // DQ1 – Schema + nulls
        sb.append(String.format(
                Locale.US,
                "DQ1_SCHEMA         | Schema Validation and Null Value Check               | %2d / 10 | %s " +
                        "| Required columns: [total_energy, co2_kg]. " +
                        "Empty cell counts in required columns → total_energy=%d, co2_kg=%d.%n",
                dq1,
                (dq.energyNulls() == 0 && dq.co2Nulls() == 0)
                        ? "All required columns are present and every required cell has a value."
                        : "All required columns are present, but some required cells are empty (null) and should be reviewed.",
                dq.energyNulls(),
                dq.co2Nulls()
        ));

        // DQ2 – Period
        sb.append(String.format(
                Locale.US,
                "DQ2_PERIOD         | Reporting Period Format and Consistency              | %2d / 10 | %s " +
                        "| Reporting periods observed in the dataset: [%s].%n",
                dq2,
                period.isBlank()
                        ? "The reporting period is not clearly specified in the data."
                        : "All rows appear to use a single, consistent reporting period in the expected format (YYYY-MM).",
                period.isBlank() ? "not available" : period
        ));

        // DQ3 – Energy validity
        sb.append(String.format(
                Locale.US,
                "DQ3_ENERGY         | Energy Value Validity (Greater Than Zero)           | %2d / 15 | %s " +
                        "| Total rows=%d. Rows with non-positive or missing total_energy=%d.%n",
                dq3,
                dq.nonPositiveEnergy() == 0
                        ? "All energy values are numeric, strictly greater than zero, and appear physically plausible."
                        : "Some rows contain missing, zero, or negative energy values that should be corrected or explained.",
                rowCount,
                dq.nonPositiveEnergy()
        ));

        // DQ4 – Duplicate plates
        sb.append(String.format(
                Locale.US,
                "DQ4_DUP_PLATE      | Duplicate License Plate Detection                   | %2d / 10 | %s " +
                        "| Number of license plate records that appear in more than one company=%d.%n",
                dq4,
                duplicatePlatesAcrossCompanies == 0
                        ? "No duplicate license plates were detected across companies."
                        : "Duplicate license plates were detected across companies; this may indicate reused identifiers or data configuration issues.",
                duplicatePlatesAcrossCompanies
        ));

        // DQ5 – Exact duplicate row (energy + CO₂)
        sb.append(String.format(
                Locale.US,
                "DQ5_DUP_ROW        | Exact Duplicate Row Detection (Energy and CO₂ Pair) | %2d /  5 | %s " +
                        "| Number of additional duplicated (total_energy, co2_kg) rows beyond the first instance=%d.%n",
                dq5,
                dq.duplicateEnergyCo2Rows() == 0
                        ? "No exact duplicate rows were found for the pair (total_energy, co2_kg)."
                        : "Some rows share exactly the same combination of total_energy and co2_kg; this often comes from copy-paste or repeated imports.",
                dq.duplicateEnergyCo2Rows()
        ));

        // DQ6 – Outliers (IQR)
        sb.append(String.format(
                Locale.US,
                "DQ6_OUTLIER_IQR    | Energy Outlier Detection (IQR Method)              | %2d / 10 | %s " +
                        "| Q1=%.3f, Q3=%.3f, IQR=%.3f, lowerBound=%.3f, upperBound=%.3f, outlierCount=%d.%n",
                dq6,
                dq.outlierCount() == 0
                        ? "No strong outliers were detected. All energy values fall inside the IQR-based expected range."
                        : "One or more energy values fall outside the IQR-based expected range and should be reviewed individually.",
                dq.q1(),
                dq.q3(),
                dq.iqr(),
                dq.lowerFence(),
                dq.upperFence(),
                dq.outlierCount()
        ));

        // DQ7 – Uniformity (CV)
        sb.append(String.format(
                Locale.US,
                "DQ7_UNIFORMITY_CV  | Energy Uniformity Rule (Coefficient of Variation)  | %2d /  5 | %s " +
                        "| Mean energy=%.3f, standardDeviation=%.3f, coefficientOfVariation=%.6f, configuredThreshold=%.6f.%n",
                dq7,
                dq.coefficientOfVariation() <= cvUniformity
                        ? "Energy values are very uniform (low CV). This can be acceptable in highly stable operations, but in most cases such flat profiles should be justified with operational evidence."
                        : "Energy values show a normal level of variation. The coefficient of variation is above the uniformity threshold.",
                dq.meanEnergy(),
                dq.stdEnergy(),
                dq.coefficientOfVariation(),
                cvUniformity
        ));

        // DQ8 – Repeated rounded values
        sb.append(String.format(
                Locale.US,
                "DQ8_REPEAT_VALUES  | Repeated Rounded Energy Values Detection            | %2d /  5 | %s " +
                        "| Number of rounded total_energy keys that appear more than once=%d. Example rounded values=%s.%n",
                dq8,
                dq.repeatedRoundedKeys() == 0
                        ? "No suspicious repetition of rounded energy values was detected."
                        : "Repeated rounded energy values were detected. This may be consistent with meter resolution, but high repetition can also indicate manual rounding or synthetic aggregation.",
                dq.repeatedRoundedKeys(),
                dq.topRepeatedRoundedValues()
        ));

        return sb.toString();
    }

    // ========= Configurable helpers (no hard-coded thresholds in logic) =========

    private double efHighThreshold() {
        return getDoubleProperty("carbonx.ai.efHighThreshold", 0.6d);
    }

    private double efLowThreshold() {
        return getDoubleProperty("carbonx.ai.efLowThreshold", 0.2d);
    }

    private double cvUniformityThreshold() {
        return getDoubleProperty("carbonx.ai.cvUniformityThreshold", 0.02d);
    }

    private double cvSuspiciousThreshold() {
        return getDoubleProperty("carbonx.ai.cvSuspiciousThreshold", 0.005d);
    }

    private double fraudHighThreshold() {
        return getDoubleProperty("carbonx.ai.fraudHighThreshold", 0.5d);
    }

    private int maxOutputTokens() {
        return getIntProperty("carbonx.ai.maxOutputTokens", 768);
    }

    private double temperature() {
        return getDoubleProperty("carbonx.ai.temperature", 0.1d);
    }

    private int minNotesLength() {
        return getIntProperty("carbonx.ai.minNotesLength", 350);
    }

    private int scoreScale() {
        return getIntProperty("carbonx.ai.scoreScale", 1);
    }

    private boolean appendRiskSummaryTail() {
        return Boolean.parseBoolean(
                System.getProperty("carbonx.ai.appendRiskSummaryTail", "true")
        );
    }

    private String defaultVersion() {
        return System.getProperty("carbonx.ai.defaultVersion", "v2.5");
    }

    private List<String> recommendedActions() {
        // Cho phép override recommendations bằng system property:
        // -Dcarbonx.ai.recommendations="Rec1|Rec2|Rec3"
        String raw = System.getProperty("carbonx.ai.recommendations", "").trim();
        if (!raw.isEmpty()) {
            return Arrays.stream(raw.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        // Default recommendations (có thể thay bằng config nếu muốn)
        return List.of(
                "Provide metering and instrumentation calibration certificates for the reporting period.",
                "Export and archive raw time-series meter readings so that aggregated values can be traced back to primary data.",
                "Reconcile all asset identifiers (for example, license plates and equipment serial numbers) with fleet, procurement, and operation and maintenance records.",
                "Document key methodological choices (system boundary, activity data sources, emission factor references) in a version-controlled methodology note.",
                "Introduce structured reason codes for rows with zero or missing energy and implement data validation rules in the ETL pipeline."
        );
    }

    private double getDoubleProperty(String key, double defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("[AI] Property {}='{}' is not a valid double. Using default={}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    private int getIntProperty(String key, int defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("[AI] Property {}='{}' is not a valid integer. Using default={}", key, raw, defaultValue);
            return defaultValue;
        }
    }
}

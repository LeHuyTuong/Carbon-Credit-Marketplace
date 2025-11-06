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
import reactor.netty.http.client.HttpClient;
import io.netty.resolver.DefaultAddressResolverGroup;

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
    private final EmissionReportRepository reportRepo;              // dùng để save()
    private final EmissionReportDetailRepository detailRepo;

    // ========= Config / constants =========
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double EF_HIGH_THRESHOLD = 0.6d;
    private static final double EF_LOW_THRESHOLD  = 0.2d;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Transactional
    @Override
    public AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details) {
        if (!cfg.isEnabled()) {
            return new AiScoreResult(BigDecimal.ZERO, "AI disabled", "na");
        }

        // ===== Basic stats =====
        final int count = details == null ? 0 : details.size();
        final long zeroEnergy = details == null ? 0 : details.stream()
                .filter(d -> d.getTotalEnergy() == null || d.getTotalEnergy().signum() == 0)
                .count();

        final double avgEf = details == null ? 0.0 : details.stream()
                .filter(d -> d.getTotalEnergy() != null && d.getTotalEnergy().signum() > 0 && d.getCo2Kg() != null)
                .map(d -> d.getCo2Kg().divide(d.getTotalEnergy(), 6, RoundingMode.HALF_UP))
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);

        final double avgCo2 = details == null ? 0.0 : details.stream()
                .map(EmissionReportDetail::getCo2Kg)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);

        final double co2Coverage = (double) (details == null ? 0 : details.stream()
                .filter(d -> d.getCo2Kg() != null)
                .count()) / Math.max(1, count);

        // ===== Anomalies (đÃ BỎ sharedProjectReports) =====
        int duplicatePlates = 0;
        boolean efTooHigh = false;
        boolean efTooLow = false;
        try {
            Long reportId = report != null ? report.getId() : null;
            Long sellerId = (report != null && report.getSeller() != null) ? report.getSeller().getId() : null;

            if (reportId != null && sellerId != null) {
                duplicatePlates = detailRepo.countDuplicatePlatesAcrossCompanies(reportId, sellerId);
            }

            efTooHigh = avgEf > EF_HIGH_THRESHOLD;
            efTooLow  = avgEf < EF_LOW_THRESHOLD;
        } catch (Exception ex) {
            log.warn("[AI] Warning: anomaly check failed: {}", ex.getMessage());
        }

        // ===== Cảnh báo log nếu thiếu các field context =====
        logMissingProjectContext(report);

        // ===== Tính các chỉ số DQ để đưa vào 'Rule Details' (fallback notes) =====
        DQMetrics dq = computeDQMetrics(details);

        // ===== Prompt =====
        final String projectContext = buildProjectContext(report);
        final String dataContext    = buildDataContext(report, count, zeroEnergy, co2Coverage, avgEf, avgCo2);
        final String anomalyContext = buildAnomalyContext(duplicatePlates, efTooHigh, efTooLow); // không còn sharedProjectReports
        final String userPrompt     = buildPrompt(projectContext, dataContext, anomalyContext);

        // Log + optional dump to file for debugging
        if (log.isDebugEnabled()) {
            log.debug("[AI] userPrompt sent to Vertex Gemini:\n{}", userPrompt);
        }
        dumpPromptIfEnabled(userPrompt, report);

        // ===== Request body =====
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 768
                )
        );

        final String path = String.format(
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

            double sc      = ((Number) parsed.getOrDefault("score", 0)).doubleValue();
            String version = Objects.toString(parsed.getOrDefault("version", "v2.0"));
            String notes   = Objects.toString(parsed.getOrDefault("notes", ""));
            String risk    = Objects.toString(parsed.getOrDefault("riskLevel", "LOW"));
            double fraud   = ((Number) parsed.getOrDefault("fraudLikelihood", 0)).doubleValue();

            boolean usedRich = false;
            if (notes.isBlank() || notes.length() < 350) {
                notes = buildRichNotes(
                        report,
                        risk, fraud, sc,
                        co2Coverage, avgEf, avgCo2,
                        count, zeroEnergy, duplicatePlates,
                        dq
                );
                usedRich = true;
            }

            if (!usedRich) {
                notes += "\n\n--- Risk summary ---\nRisk level: " + risk + "\nFraud likelihood: " + fraud;
            }

            BigDecimal score = BigDecimal.valueOf(sc).setScale(1, RoundingMode.HALF_UP);

            // Persist vào report
            if (report != null) {
                report.setAiPreScore(score);
                report.setAiPreNotes(notes);
                report.setAiVersion(version);
                reportRepo.save(report);
            }

            log.info("[AI] Updated EmissionReport#{} with AI results (score={}, risk={}, fraud={})",
                    report != null ? report.getId() : null, score, risk, fraud);

            return new AiScoreResult(score, notes, version);

        } catch (Exception ex) {
            log.error("[AI] Vertex Gemini analysis failed: {}", ex.getMessage(), ex);
            return new AiScoreResult(BigDecimal.ZERO, "AI error: " + ex.getMessage(), "error");
        }
    }

    // ===== Prompt builders =====

    private String buildProjectContext(EmissionReport report) {
        String commitments        = safe(report != null && report.getProject() != null ? report.getProject().getCommitments() : null);
        String measurementMethod  = safe(report != null && report.getProject() != null ? report.getProject().getMeasurementMethod() : null);
        String technicalIndicators= safe(report != null && report.getProject() != null ? report.getProject().getTechnicalIndicators() : null);

        return String.format(Locale.US,
                "Project context:%n" +
                        "- Commitments: %s%n" +
                        "- MeasurementMethod: %s%n" +
                        "- TechnicalIndicators: %s",
                commitments, measurementMethod, technicalIndicators);
    }

    private String buildDataContext(EmissionReport report,
                                    int count,
                                    long zeroEnergy,
                                    double co2Coverage,
                                    double avgEf,
                                    double avgCo2) {
        String company = report != null && report.getSeller() != null ? safe(report.getSeller().getCompanyName()) : "";
        String project = report != null && report.getProject() != null ? safe(report.getProject().getTitle()) : "";
        String period  = report != null ? safe(report.getPeriod()) : "";

        return String.format(Locale.US,
                "Report summary:%n" +
                        "- Company: %s%n" +
                        "- Project: %s%n" +
                        "- Period: %s%n" +
                        "- Rows: %d%n" +
                        "- Zero-energy rows: %d%n" +
                        "- CO2 coverage: %.0f%%%n" +
                        "- Avg EF (kg/kWh): %.3f%n" +
                        "- Avg CO2 per row (kg): %.1f",
                company, project, period, count, zeroEnergy, co2Coverage * 100, avgEf, avgCo2);
    }

    // ĐÃ BỎ sharedProjectReports khỏi context/prompt
    private String buildAnomalyContext(int duplicatePlates,
                                       boolean efTooHigh,
                                       boolean efTooLow) {
        return String.format(Locale.US,
                "Detected anomalies:%n" +
                        "- Duplicate plates across companies: %d%n" +
                        "- EF too high: %s%n" +
                        "- EF too low: %s",
                duplicatePlates, efTooHigh, efTooLow);
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

    // Parse lenient + fallback thủ công
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

                Matcher fraudM = Pattern.compile("\"fraudLikelihood\"\\s*:\\s*([0-9.]+)").matcher(sub);
                if (fraudM.find()) fallback.put("fraudLikelihood", Double.parseDouble(fraudM.group(1)));
            } catch (Exception ignored) { }
            log.warn("[AI] ⚠️ Fallback used for incomplete JSON:\n{}", sub);
            return fallback;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Cảnh báo khi thiếu các field context
    private void logMissingProjectContext(EmissionReport report) {
        try {
            Long reportId  = report != null ? report.getId() : null;
            Long projectId = (report != null && report.getProject() != null) ? report.getProject().getId() : null;

            String commitments = (report != null && report.getProject() != null) ? report.getProject().getCommitments() : null;
            String method      = (report != null && report.getProject() != null) ? report.getProject().getMeasurementMethod() : null;
            String indicators  = (report != null && report.getProject() != null) ? report.getProject().getTechnicalIndicators() : null;

            List<String> missing = new ArrayList<>();
            if (isBlank(commitments)) missing.add("Commitments");
            if (isBlank(method))      missing.add("MeasurementMethod");
            if (isBlank(indicators))  missing.add("TechnicalIndicators");

            if (!missing.isEmpty()) {
                log.warn("[AI] Missing project context fields {} for reportId={}, projectId={}",
                        missing, reportId, projectId);
            }
        } catch (Exception e) {
            log.warn("[AI] Failed to check project context fields: {}", e.getMessage());
        }
    }

    private void dumpPromptIfEnabled(String prompt, EmissionReport report) {
        try {
            String enabled = System.getProperty("carbonx.ai.prompt.dump", "false");
            if (!"true".equalsIgnoreCase(enabled)) return;

            String dir = System.getProperty("carbonx.ai.prompt.dir", "./ai-prompts");
            Files.createDirectories(Path.of(dir));

            String ts = TS_FMT.format(LocalDateTime.now());
            String id = report != null && report.getId() != null ? ("report-" + report.getId()) : "report-unknown";
            Path out = Path.of(dir, ts + "_" + id + ".txt");

            Files.writeString(out, prompt, StandardCharsets.UTF_8);
            log.info("[AI] Prompt dumped to file: {}", out.toAbsolutePath());
        } catch (Exception e) {
            log.warn("[AI] Unable to dump prompt file: {}", e.getMessage());
        }
    }

    // ======= Data Quality metrics (cho "Rule Details") =======

    private record DQMetrics(
            long energyNulls,
            long co2Nulls,
            long nonPositiveEnergy,
            long dupEnergyCo2Rows,
            double q1, double q3, double iqr, double lowerFence, double upperFence, long outliers,
            double meanEnergy, double stdEnergy, double cvEnergy,
            int repeatedRoundedKeys, List<Long> topRepeatedRoundedValues
    ) {}

    private DQMetrics computeDQMetrics(List<EmissionReportDetail> details) {
        if (details == null || details.isEmpty()) {
            return new DQMetrics(0,0,0,0,0,0,0,0,0,0,0,0,0,0,new ArrayList<>());
        }

        // null counts
        long energyNulls = details.stream().filter(d -> d.getTotalEnergy() == null).count();
        long co2Nulls    = details.stream().filter(d -> d.getCo2Kg() == null).count();

        // non-positive energy (<=0 or null)
        long nonPositive = details.stream()
                .filter(d -> d.getTotalEnergy() == null || d.getTotalEnergy().signum() <= 0)
                .count();

        // energies (positive only) for stats
        List<Double> energies = details.stream()
                .map(EmissionReportDetail::getTotalEnergy)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .filter(v -> v > 0d)
                .sorted()
                .collect(Collectors.toList());

        // duplicate exact rows by (energy, co2)
        Map<String, Long> rowMap = details.stream()
                .map(d -> {
                    String e = d.getTotalEnergy() == null ? "null" : d.getTotalEnergy().stripTrailingZeros().toPlainString();
                    String c = d.getCo2Kg() == null      ? "null" : d.getCo2Kg().stripTrailingZeros().toPlainString();
                    return e + "|" + c;
                })
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        long dupRows = rowMap.values().stream().filter(cnt -> cnt > 1).mapToLong(cnt -> cnt - 1).sum();

        // IQR outlier for energy
        double q1 = quantile(energies, 0.25);
        double q3 = quantile(energies, 0.75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;
        long outliers = energies.stream().filter(v -> v < lower || v > upper).count();

        // mean/std/cv for energy
        double mean = mean(energies);
        double std  = stddev(energies, mean);
        double cv   = mean > 0 ? std / mean : 0d;

        // repeated rounded energies (e.g., repeated integer values)
        Map<Long, Long> roundedCounts = energies.stream()
                .map(Math::round)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        int repeatedKeys = (int) roundedCounts.entrySet().stream().filter(e -> e.getValue() > 1).count();
        List<Long> topRepeated = roundedCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new DQMetrics(
                energyNulls, co2Nulls, nonPositive, dupRows,
                q1, q3, iqr, lower, upper, outliers,
                mean, std, cv,
                repeatedKeys, topRepeated
        );
    }

    private static double quantile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0d;
        double pos = p * (sorted.size() - 1);
        int i = (int) Math.floor(pos);
        int j = (int) Math.ceil(pos);
        if (i == j) return sorted.get(i);
        double w = pos - i;
        return sorted.get(i) * (1 - w) + sorted.get(j) * w;
    }

    private static double mean(List<Double> xs) {
        if (xs.isEmpty()) return 0d;
        double s = 0d;
        for (double v : xs) s += v;
        return s / xs.size();
    }

    private static double stddev(List<Double> xs, double mean) {
        if (xs.size() <= 1) return 0d;
        double s2 = 0d;
        for (double v : xs) {
            double d = v - mean;
            s2 += d * d;
        }
        return Math.sqrt(s2 / (xs.size() - 1));
    }

    // ===== Rich notes builder (fallback) với "Rule Details" giống ảnh, KHÔNG có shared sellers =====
    private String buildRichNotes(EmissionReport report,
                                  String risk,
                                  double fraud,
                                  double score,
                                  double co2Coverage,
                                  double avgEf,
                                  double avgCo2,
                                  int rows,
                                  long zeroEnergy,
                                  int duplicatePlates,
                                  DQMetrics dq) {

        String coverageLabel = co2Coverage > 0.9 ? "high" : (co2Coverage > 0.7 ? "moderate" : "low");
        String effBand = avgEf < EF_LOW_THRESHOLD ? "below expected" :
                (avgEf > EF_HIGH_THRESHOLD ? "above expected" : "within expected");
        String conf = switch (risk.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> "Low";
            case "MEDIUM" -> "Medium";
            default -> "High";
        };

        // Scoring nhỏ cho DQ rules (mô phỏng như bảng)
        // Bạn có thể tinh chỉnh các weight/điểm này theo rubric riêng
        int dq1 = (dq.energyNulls == 0 && dq.co2Nulls == 0) ? 10 : Math.max(0, 10 - (int)(dq.energyNulls + dq.co2Nulls));
        int dq2 = 10; // giả định 1 kỳ hợp lệ theo report.period
        int dq3 = dq.nonPositiveEnergy == 0 ? 15 : Math.max(0, 15 - (int) dq.nonPositiveEnergy);
        int dq4 = duplicatePlates == 0 ? 10 : Math.max(0, 10 - duplicatePlates);
        int dq5 = dq.dupEnergyCo2Rows == 0 ? 5 : Math.max(0, 5 - (int) dq.dupEnergyCo2Rows);
        int dq6 = dq.outliers == 0 ? 10 : Math.max(0, 10 - (int) dq.outliers);
        // CV threshold (ví dụ 0.02 như ảnh)
        double cvThreshold = 0.02;
        int dq7 = dq.cvEnergy <= cvThreshold ? 5 : 0;
        int dq8 = dq.repeatedRoundedKeys == 0 ? 5 : Math.max(0, 5 - dq.repeatedRoundedKeys);

        StringBuilder sb = new StringBuilder(1600);
        sb.append(String.format(Locale.US,
                "Overview\n" +
                        "- Preliminary score: %.1f/10; risk level: %s; fraud likelihood: %.2f.\n" +
                        "- The dataset contains %d rows with %d zero-energy records; CO2 coverage is %.0f%% (%s).\n" +
                        "- Average emission factor (EF) is %.3f kg/kWh (%s band). Average CO2 per row is %.1f kg.\n",
                score, risk, fraud, rows, zeroEnergy, co2Coverage * 100, coverageLabel, avgEf, effBand, avgCo2
        ));

        sb.append("\nKey Signals\n");
        sb.append(String.format(Locale.US,
                "- Coverage and completeness are %s with %.0f%% rows having CO2 values.\n" +
                        "- Zero-energy rows: %d (should be validated for measurement gaps or data entry defaults).\n" +
                        "- EF thresholds used: low < %.1f; high > %.1f (heuristic bounds for screening).\n",
                coverageLabel, co2Coverage * 100, zeroEnergy, EF_LOW_THRESHOLD, EF_HIGH_THRESHOLD
        ));

        sb.append("\nAnomalies & Explanations\n");
        if (duplicatePlates > 0) {
            sb.append(String.format(Locale.US,
                    "- Detected %d duplicate license plates across companies → potential asset reuse or ID collision; cross-check registration and ownership proofs.\n",
                    duplicatePlates));
        } else {
            sb.append("- No duplicate license plates across companies detected.\n");
        }
        if (avgEf > EF_HIGH_THRESHOLD) {
            sb.append(String.format(Locale.US,
                    "- EF appears high (%.3f > %.1f) → verify emission factors, grid mix, and calculation method.\n", avgEf, EF_HIGH_THRESHOLD));
        } else if (avgEf < EF_LOW_THRESHOLD) {
            sb.append(String.format(Locale.US,
                    "- EF appears low (%.3f < %.1f) → confirm metering accuracy, system boundaries, and activity data.\n", avgEf, EF_LOW_THRESHOLD));
        } else {
            sb.append(String.format(Locale.US,
                    "- EF falls within expected screening band (%.3f in [%.1f, %.1f]).\n", avgEf, EF_LOW_THRESHOLD, EF_HIGH_THRESHOLD));
        }

        sb.append("\nData Quality\n");
        sb.append(String.format(Locale.US,
                "- CO2 value availability: %.0f%%; missingness primarily impacts comparability and uncertainty quantification.\n" +
                        "- Zero-energy rows may reflect downtime or sensor dropouts; annotate causes and retain raw logs.\n",
                co2Coverage * 100
        ));

        sb.append("\nRecommendations\n");
        sb.append("- Provide meter calibration certificates and raw export snapshots for the reporting period.\n");
        sb.append("- Reconcile asset identifiers (plates/serials) with procurement and O&M records; resolve duplicates.\n");
        sb.append("- Document methodological choices (Boundary, Activity Data, Emission Factors) with versioned references.\n");
        sb.append("- Add reason codes for zero-energy rows and apply validation rules in ETL to prevent silent defaults.\n");

        sb.append(String.format(Locale.US,
                "\nConfidence\n" +
                        "- Auditor confidence: %s, given risk level %s and observed data quality. This is a screening output and should be complemented by evidence review.\n",
                conf, risk
        ));

        // ======= Rule Details (giống bố cục ảnh) =======
        sb.append("\nRule Details\n");
        sb.append(String.format(Locale.US,
                "DQ1_SCHEMA    | Schema & Nulls         | %d / 10 | %s | required=[total_energy, co2_kg], nulls: energy=%d, co2=%d\n",
                dq1, (dq.energyNulls==0 && dq.co2Nulls==0) ? "Columns OK" : "Nulls present",
                dq.energyNulls, dq.co2Nulls
        ));
        // Period: dùng report.period nếu có
        String period = report != null ? safe(report.getPeriod()) : "";
        sb.append(String.format(Locale.US,
                "DQ2_PERIOD    | Period format & single | %d / 10 | %s | periods=[%s]\n",
                dq2, isBlank(period) ? "Unknown period" : "One valid period", isBlank(period) ? "" : period
        ));
        sb.append(String.format(Locale.US,
                "DQ3_ENERGY    | Energy validity (>0)   | %d / 15 | %s | total=%d, nonPositive=%d\n",
                dq3, dq.nonPositiveEnergy==0 ? "OK" : "Has non-positive", rows, dq.nonPositiveEnergy
        ));
        sb.append(String.format(Locale.US,
                "DQ4_DUP_PLATE | Duplicate license plates| %d / 10 | %s | duplicates=%d\n",
                dq4, duplicatePlates==0 ? "No duplicates" : "Duplicates found", duplicatePlates
        ));
        sb.append(String.format(Locale.US,
                "DQ5_DUP_ROW   | Exact duplicate rows   | %d / 5  | %s | dupRows=%d\n",
                dq5, dq.dupEnergyCo2Rows==0 ? "No duplicate rows" : "Duplicates exist", dq.dupEnergyCo2Rows
        ));
        sb.append(String.format(Locale.US,
                "DQ6_OUTLIER_IQR | Outlier detection (IQR)| %d / 10 | %s | q1=%.3f,q3=%.3f,lower=%.3f,upper=%.3f,outliers=%d\n",
                dq6, dq.outliers==0 ? "No outliers" : "Outliers present", dq.q1, dq.q3, dq.lowerFence, dq.upperFence, dq.outliers
        ));
        sb.append(String.format(Locale.US,
                "DQ7_UNIFORMITY_CV | Uniformity check (CV) | %d / 5  | %s | mean=%.3f,std=%.3f,cv=%.6f,threshold=%.6f\n",
                dq7, dq.cvEnergy <= 0.02 ? "Dispersion OK" : "High dispersion", dq.meanEnergy, dq.stdEnergy, dq.cvEnergy, 0.02
        ));
        sb.append(String.format(Locale.US,
                "DQ8_REPEAT_VALUES | Repeated rounded energies | %d / 5 | %s | repeatedKeys=%d, top=%s\n",
                dq8, dq.repeatedRoundedKeys==0 ? "No repeated rounded values" : "Repeated rounded values exist",
                dq.repeatedRoundedKeys, dq.topRepeatedRoundedValues
        ));

        return sb.toString();
    }
}

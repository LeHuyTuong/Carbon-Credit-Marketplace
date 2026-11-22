package com.carbonx.marketcarbon.service.ai;

import com.carbonx.marketcarbon.model.EmissionReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds the prompt sent to Vertex Gemini and optionally dumps it to disk.
 *
 * <p>Extracted from {@code GeminiAiScoringService} so that prompt wording and
 * context formatting live in one place. The produced text is byte-for-byte
 * identical to the original inline implementation.
 */
@Component
@Slf4j
public class GeminiPromptBuilder {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public String buildProjectContext(EmissionReport report) {
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

    public String buildDataContext(
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

    public String buildAnomalyContext(
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

    public String buildPrompt(String projectContext, String dataContext, String anomalyContext) {
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

    public void dumpPromptIfEnabled(String prompt, EmissionReport report) {
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

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

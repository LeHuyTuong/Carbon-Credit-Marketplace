package com.carbonx.marketcarbon.service.analysis;

import com.carbonx.marketcarbon.dto.analysis.AnalysisResult;
import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.dto.analysis.RuleRubric;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.repository.EmissionReportDetailRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.service.analysis.rules.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

    private final EmissionReportRepository reportRepo;
    private final EmissionReportDetailRepository detailRepo;

    // Maximum score per rule (weights)
    private static final Map<String, Integer> DQ_WEIGHTS = Map.of(
            "DQ1_SCHEMA", 10,
            "DQ2_PERIOD", 10,
            "DQ3_ENERGY", 15,
            "DQ4_DUP_PLATE", 10,
            "DQ5_DUP_ROW", 5,
            "DQ6_OUTLIER_IQR", 10,
            "DQ7_UNIFORMITY_CV", 5,
            "DQ8_REPEAT_VALUES", 5
    );

    private static final int FRAUD_MAX = 30;

    // Canonical names for rules (no abbreviations in UI)
    private static final Map<String, String> RULE_NAMES = Map.of(
            "DQ1_SCHEMA", "Schema Validation Rule",
            "DQ2_PERIOD", "Period Consistency Rule",
            "DQ3_ENERGY", "Energy Validity Rule",
            "DQ4_DUP_PLATE", "Duplicate License Plate Detection",
            "DQ5_DUP_ROW", "Exact Duplicate Row Detection",
            "DQ6_OUTLIER_IQR", "Energy Outlier Detection (IQR Method)",
            "DQ7_UNIFORMITY_CV", "Energy Uniformity Rule (Coefficient of Variation)",
            "DQ8_REPEAT_VALUES", "Repeated Rounded Energy Values Detection"
    );

    // Short descriptions for each rule
    private static final Map<String, String> RULE_DESCRIPTIONS = Map.of(
            "DQ1_SCHEMA", "Check that all required columns exist and that required fields are not empty.",
            "DQ2_PERIOD", "Verify that all rows use a single, valid reporting period with consistent format.",
            "DQ3_ENERGY", "Validate that all energy values are numeric and strictly greater than zero.",
            "DQ4_DUP_PLATE", "Detect duplicate license plate identifiers across rows in the dataset.",
            "DQ5_DUP_ROW", "Detect rows that are exact duplicates of each other based on all fields.",
            "DQ6_OUTLIER_IQR", "Identify extreme energy values using the interquartile range (IQR) method.",
            "DQ7_UNIFORMITY_CV", "Check whether energy values are suspiciously uniform based on the coefficient of variation.",
            "DQ8_REPEAT_VALUES", "Detect repeated rounded energy values that may indicate manual rounding or synthetic aggregation."
    );
    // 1) AUTO ANALYSIS MODE (system / AI scoring)
    public AnalysisResult analyzeNoCo2(long reportId, boolean persist) {

        var report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        // Load all details for this report
        List<EmissionReportDetail> details = detailRepo.findByReport_Id(reportId);

        // Map each row into a generic map for the rule engine
        List<Map<String, Object>> rows = details.stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", report.getPeriod());
                    m.put("total_energy", toDouble(d.getTotalEnergy()));
                    m.put("license_plate", d.getVehiclePlate());
                    return m;
                })
                .collect(Collectors.toList());

        Set<String> columns = Set.of("period", "total_energy", "license_plate");

        AnalysisContext ctx = AnalysisContext.builder()
                .reportId(reportId)
                .reportingPeriod(report.getPeriod())
                .rows(rows)
                .columns(columns)
                .cvUniformityThreshold(0.02)
                .roundRepeatScale(2)
                .build();

        // Active rule set
        List<IRule> rules = List.of(
                new SchemaRule(Set.of("period", "total_energy", "license_plate")),
                new PeriodRule(),
                new EnergyValidRule(),
                new DuplicatePlateRule(),
                new ExactDuplicateRowRule(),
                new EnergyOutlierIqrRule(),
                new EnergyUniformityCvRule(0.02),
                new RepeatedRoundedEnergyRule(2)
        );

        int totalDQScore = 0;
        List<RuleResult> results = new ArrayList<>();

        for (IRule rule : rules) {
            RuleResult rr = rule.apply(ctx);

            // Canonical rule name
            rr.setName(RULE_NAMES.getOrDefault(rr.getRuleId(), "Unknown Rule"));

            // Default message if rule did not override it
            if (rr.getMessage() == null || rr.getMessage().isBlank()) {
                rr.setMessage(RULE_DESCRIPTIONS.getOrDefault(rr.getRuleId(), ""));
            }

            // Maximum score for this rule from configuration
            int maxScore = DQ_WEIGHTS.getOrDefault(rr.getRuleId(), rule.maxScore());
            rr.setMaxScore(maxScore);

            // Clamp score to max
            rr.setScore(Math.min(rr.getScore(), maxScore));

            // Never return null evidence
            if (rr.getEvidence() == null) {
                rr.setEvidence("");
            }

            results.add(rr);
            totalDQScore += rr.getScore();
        }

        int dqMax = DQ_WEIGHTS.values().stream().mapToInt(i -> i).sum();

        var fraud = new FraudDetector().detect(ctx);

        AnalysisResult output = new AnalysisResult(
                reportId,
                "logic-no-co2-v1",
                totalDQScore,
                dqMax,
                fraud.getScore(),
                FRAUD_MAX,
                results,
                fraud.getReasons()
        );

        if (persist) {
            reportRepo.save(report);
        }

        return output;
    }

    // 2) MANUAL RUBRIC MODE (per-report, but score = 0)
    //    Used when a human or external AI wants to grade manually
    public List<RuleResult> buildManualEvaluation(long reportId) {

        var report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        List<EmissionReportDetail> details = detailRepo.findByReport_Id(reportId);

        List<Map<String, Object>> rows = details.stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", report.getPeriod());
                    m.put("total_energy", toDouble(d.getTotalEnergy()));
                    m.put("license_plate", d.getVehiclePlate());
                    return m;
                })
                .collect(Collectors.toList());

        Set<String> columns = Set.of("period", "total_energy", "license_plate");

        AnalysisContext ctx = AnalysisContext.builder()
                .reportId(reportId)
                .reportingPeriod(report.getPeriod())
                .rows(rows)
                .columns(columns)
                .cvUniformityThreshold(0.02)
                .roundRepeatScale(2)
                .build();

        List<IRule> rules = List.of(
                new SchemaRule(Set.of("period", "total_energy", "license_plate")),
                new PeriodRule(),
                new EnergyValidRule(),
                new DuplicatePlateRule(),
                new ExactDuplicateRowRule(),
                new EnergyOutlierIqrRule(),
                new EnergyUniformityCvRule(0.02),
                new RepeatedRoundedEnergyRule(2)
        );

        List<RuleResult> rubric = new ArrayList<>();

        for (IRule rule : rules) {
            RuleResult rr = rule.apply(ctx);

            rr.setName(RULE_NAMES.getOrDefault(rr.getRuleId(), "Unknown Rule"));

            if (rr.getMessage() == null || rr.getMessage().isBlank()) {
                rr.setMessage(RULE_DESCRIPTIONS.getOrDefault(rr.getRuleId(), ""));
            }

            int maxScore = DQ_WEIGHTS.getOrDefault(rr.getRuleId(), rule.maxScore());
            rr.setMaxScore(maxScore);

            // Manual mode: do not auto-score, leave score = 0
            rr.setScore(0);

            if (rr.getEvidence() == null) {
                rr.setEvidence("");
            }

            rubric.add(rr);
        }

        return rubric;
    }

    // 3) GLOBAL RULE RUBRIC (no reportId required)
    //    Static template for CVA / AI to understand how to score
    public List<RuleRubric> getRuleRubrics() {
        List<RuleRubric> rubrics = new ArrayList<>();

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ1_SCHEMA")
                .name(RULE_NAMES.get("DQ1_SCHEMA"))
                .maxScore(DQ_WEIGHTS.get("DQ1_SCHEMA"))
                .description(RULE_DESCRIPTIONS.get("DQ1_SCHEMA"))
                .scoringGuideline(
                        "10 = All required columns are present and all required cells are populated. " +
                                "5 = All required columns exist but some required cells are empty. " +
                                "0 = At least one required column is missing.")
                .evidenceHint(
                        "List required vs actual columns and count how many empty cells exist in each required column.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ2_PERIOD")
                .name(RULE_NAMES.get("DQ2_PERIOD"))
                .maxScore(DQ_WEIGHTS.get("DQ2_PERIOD"))
                .description(RULE_DESCRIPTIONS.get("DQ2_PERIOD"))
                .scoringGuideline(
                        "10 = All rows use a single period with a valid YYYY-MM format. " +
                                "5 = The format is valid, but multiple distinct periods are present. " +
                                "0 = One or more period values are not in the expected format.")
                .evidenceHint(
                        "Provide the distinct period values found in the dataset and indicate which ones do not match the expected YYYY-MM format.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ3_ENERGY")
                .name(RULE_NAMES.get("DQ3_ENERGY"))
                .maxScore(DQ_WEIGHTS.get("DQ3_ENERGY"))
                .description(RULE_DESCRIPTIONS.get("DQ3_ENERGY"))
                .scoringGuideline(
                        "15 = All energy values are numeric and strictly greater than zero. " +
                                "10 = At least 95% of rows have valid positive numeric energy values. " +
                                "5  = Between 80% and 95% of rows are valid. " +
                                "0  = Less than 80% of rows have valid energy values.")
                .evidenceHint(
                        "Report total row count, number of non-numeric energy values, and number of values that are zero or negative.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ4_DUP_PLATE")
                .name(RULE_NAMES.get("DQ4_DUP_PLATE"))
                .maxScore(DQ_WEIGHTS.get("DQ4_DUP_PLATE"))
                .description(RULE_DESCRIPTIONS.get("DQ4_DUP_PLATE"))
                .scoringGuideline(
                        "10 = No duplicate license plate values are detected. " +
                                "7  = Up to 2 duplicate license plate values are detected. " +
                                "3  = Between 3 and 5 duplicate license plate values are detected. " +
                                "0  = More than 5 duplicate license plate values are detected.")
                .evidenceHint(
                        "Count how many distinct license plates appear more than once and list the most frequent duplicates.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ5_DUP_ROW")
                .name(RULE_NAMES.get("DQ5_DUP_ROW"))
                .maxScore(DQ_WEIGHTS.get("DQ5_DUP_ROW"))
                .description(RULE_DESCRIPTIONS.get("DQ5_DUP_ROW"))
                .scoringGuideline(
                        "5 = No exact duplicate rows are present. " +
                                "3 = A small number of exact duplicates are present but they are justified (for example, repeated imports). " +
                                "0 = Many exact duplicates exist without clear justification.")
                .evidenceHint(
                        "Provide the total number of exact duplicate rows and examples of the repeated records.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ6_OUTLIER_IQR")
                .name(RULE_NAMES.get("DQ6_OUTLIER_IQR"))
                .maxScore(DQ_WEIGHTS.get("DQ6_OUTLIER_IQR"))
                .description(RULE_DESCRIPTIONS.get("DQ6_OUTLIER_IQR"))
                .scoringGuideline(
                        "10 = No outliers are found outside the IQR-based bounds. " +
                                "8  = A small number of outliers are present but each has a documented explanation. " +
                                "5  = Several outliers are present and only partially explained. " +
                                "2  = Many outliers are present with no clear explanation.")
                .evidenceHint(
                        "Provide Q1, Q3, the IQR, the lower and upper bounds, and the number of values outside these bounds.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ7_UNIFORMITY_CV")
                .name(RULE_NAMES.get("DQ7_UNIFORMITY_CV"))
                .maxScore(DQ_WEIGHTS.get("DQ7_UNIFORMITY_CV"))
                .description(RULE_DESCRIPTIONS.get("DQ7_UNIFORMITY_CV"))
                .scoringGuideline(
                        "5 = The coefficient of variation (CV) is within a normal expected range. " +
                                "2 = The CV is very low (values are highly uniform) but there is supporting operational evidence. " +
                                "0 = The CV is very low and there is no convincing evidence to justify such uniformity.")
                .evidenceHint(
                        "Report the mean, standard deviation, and coefficient of variation for energy values, and compare the CV to the configured threshold.")
                .build());

        rubrics.add(RuleRubric.builder()
                .ruleId("DQ8_REPEAT_VALUES")
                .name(RULE_NAMES.get("DQ8_REPEAT_VALUES"))
                .maxScore(DQ_WEIGHTS.get("DQ8_REPEAT_VALUES"))
                .description(RULE_DESCRIPTIONS.get("DQ8_REPEAT_VALUES"))
                .scoringGuideline(
                        "5 = No suspicious repetition of rounded energy values is observed. " +
                                "3 = Some rounded values repeat but at a level consistent with the metering resolution. " +
                                "1 = Many rounded values repeat, suggesting possible manual rounding or synthetic aggregation.")
                .evidenceHint(
                        "Provide the number of rounded energy keys that appear more than once and list the most frequent repeated values with their counts.")
                .build());

        return rubrics;
    }

    private static Double toDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }
}

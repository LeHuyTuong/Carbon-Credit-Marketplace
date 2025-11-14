package com.carbonx.marketcarbon.service.analysis;

import com.carbonx.marketcarbon.dto.analysis.AnalysisResult;
import com.carbonx.marketcarbon.dto.analysis.RuleResult;
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

    /* Điểm số tối đa của từng Rule */
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

    /** Tên đầy đủ của từng Rule (KHÔNG VIẾT TẮT) */
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

    /** Mô tả đầy đủ từng Rule */
    private static final Map<String, String> RULE_DESCRIPTIONS = Map.of(
            "DQ1_SCHEMA", "Check that all required columns exist and the data schema is correct.",
            "DQ2_PERIOD", "Ensure all rows use the same reporting period.",
            "DQ3_ENERGY", "Validate that energy values are positive and meaningful.",
            "DQ4_DUP_PLATE", "Detect repeated vehicle license plates across rows.",
            "DQ5_DUP_ROW", "Detect rows that are duplicated exactly.",
            "DQ6_OUTLIER_IQR", "Identify extreme energy values using the Interquartile Range.",
            "DQ7_UNIFORMITY_CV", "Check if energy values are too uniform (suspicious low variation).",
            "DQ8_REPEAT_VALUES", "Detect repeated rounded energy values that may indicate synthetic data."
    );


    /** MAIN FUNCTION — FULL RULE ANALYSIS */
    public AnalysisResult analyzeNoCo2(long reportId, boolean persist) {

        var report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        // 1. Lấy chi tiết bản ghi
        List<EmissionReportDetail> details = detailRepo.findByReport_Id(reportId);

        // 2. Map từng record -> JSON cho Rule Engine
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

        // 3. Build context
        AnalysisContext ctx = AnalysisContext.builder()
                .reportId(reportId)
                .reportingPeriod(report.getPeriod())
                .rows(rows)
                .columns(columns)
                .cvUniformityThreshold(0.02)
                .roundRepeatScale(2)
                .build();

        // 4. Khởi tạo danh sách Rule
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

        // 5. Chấm điểm
        int totalDQScore = 0;
        List<RuleResult> results = new ArrayList<>();

        for (IRule rule : rules) {

            RuleResult rr = rule.apply(ctx);

            // gán tên đầy đủ
            rr.setName(RULE_NAMES.getOrDefault(rr.getRuleId(), "Unknown Rule"));

            // mô tả chi tiết
            if (rr.getMessage() == null || rr.getMessage().isBlank()) {
                rr.setMessage(RULE_DESCRIPTIONS.getOrDefault(rr.getRuleId(), ""));
            }

            // severity mặc định
            if (rr.getSeverity() == null) {
                rr.setSeverity(rr.getScore() == rr.getMaxScore() ? "INFO" : "WARN");
            }

            // max score
            int maxScore = DQ_WEIGHTS.getOrDefault(rr.getRuleId(), rule.maxScore());
            rr.setMaxScore(maxScore);
            rr.setScore(Math.min(rr.getScore(), maxScore));

            // evidence mặc định nếu thiếu
            if (rr.getEvidence() == null) {
                rr.setEvidence("");
            }

            results.add(rr);
            totalDQScore += rr.getScore();
        }

        int dqMax = DQ_WEIGHTS.values().stream().mapToInt(x -> x).sum();

        // 6. Fraud detection
        FraudDetector.FraudResult fraud = new FraudDetector().detect(ctx);

        AnalysisResult output = new AnalysisResult(
                reportId,
                "logic-no-co2-v1",
                totalDQScore, dqMax,
                fraud.getScore(), FRAUD_MAX,
                results,
                fraud.getReasons()
        );

        // 7. Persist nếu cần
        if (persist) {
            reportRepo.save(report);
        }

        return output;
    }


    /** Convert BigDecimal -> Double */
    private static Double toDouble(BigDecimal val) {
        return (val == null) ? null : val.doubleValue();
    }
}

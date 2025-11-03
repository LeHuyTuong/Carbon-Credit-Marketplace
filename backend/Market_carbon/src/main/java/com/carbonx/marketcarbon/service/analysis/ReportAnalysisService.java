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

    private static final Map<String,Integer> DQ_WEIGHTS = Map.of(
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

    public AnalysisResult analyzeNoCo2(long reportId, boolean persist){
        var report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        // 1) Lấy chi tiết từ DB
        List<EmissionReportDetail> details = detailRepo.findByReport_Id(reportId);

        // 2) Map -> rows cho rule engine (period, total_energy, license_plate)
        List<Map<String,Object>> rows = details.stream()
                .map(d -> {
                    Map<String,Object> m = new HashMap<>();
                    m.put("period", report.getPeriod());
                    m.put("total_energy", toDouble(d.getTotalEnergy()));   // BigDecimal -> Double (hoặc null)
                    // đổi getter này cho đúng tên field thật trong EmissionReportDetail
                    m.put("license_plate", d.getVehiclePlate());
                    return m;
                })
                .collect(Collectors.toList());

        Set<String> columns = Set.of("period","total_energy","license_plate");

        // 3) Build context
        AnalysisContext ctx = AnalysisContext.builder()
                .reportId(reportId)
                .reportingPeriod(report.getPeriod())
                .rows(rows)
                .columns(columns)
                .cvUniformityThreshold(0.02)
                .roundRepeatScale(2)
                .build();

        // 4) Rules (no-CO2)
        List<IRule> rules = List.of(
                new SchemaRule(Set.of("period","total_energy","license_plate")),
                new PeriodRule(),
                new EnergyValidRule(),
                new DuplicatePlateRule(),
                new ExactDuplicateRowRule(),
                new EnergyOutlierIqrRule(),
                new EnergyUniformityCvRule(0.02),
                new RepeatedRoundedEnergyRule(2)
        );

        // 5) Run rules
        int dqScore = 0;
        List<RuleResult> detailsOut = new ArrayList<>();
        for (IRule r : rules){
            RuleResult rr = r.apply(ctx);
            int max = DQ_WEIGHTS.getOrDefault(r.id(), r.maxScore());
            rr.setMaxScore(max);
            rr.setScore(Math.min(rr.getScore(), max));
            detailsOut.add(rr);
            dqScore += rr.getScore();
        }
        int dqMax = DQ_WEIGHTS.values().stream().mapToInt(i->i).sum();

        // 6) Fraud-lite
        FraudDetector.FraudResult fr = new FraudDetector().detect(ctx);

        AnalysisResult ar = new AnalysisResult(
                reportId,
                "logic-no-co2-v1",
                dqScore, dqMax,
                fr.getScore(), FRAUD_MAX,
                detailsOut,
                fr.getReasons()
        );

        if (persist){
            // ví dụ bạn muốn lưu điểm vào report:
            // report.setVerificationScore(BigDecimal.valueOf( (dqScore*1.0/dqMax)*10 ).setScale(2, RoundingMode.HALF_UP));
            reportRepo.save(report);
        }
        return ar;
    }

    private static Double toDouble(BigDecimal v){
        return (v == null) ? null : v.doubleValue();
    }
}
package com.carbonx.marketcarbon.service.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class AnalysisContext {
    private long reportId;
    private String reportingPeriod;                 // "2026-10"
    private List<Map<String, Object>> rows;        // {period, total_energy, license_plate}
    private Set<String> columns;                   // tập tên cột (có thể null)

    // tuning
    @Builder.Default private double cvUniformityThreshold = 0.02; // 2%
    @Builder.Default private int roundRepeatScale = 2;             // làm tròn 2 chữ số
}
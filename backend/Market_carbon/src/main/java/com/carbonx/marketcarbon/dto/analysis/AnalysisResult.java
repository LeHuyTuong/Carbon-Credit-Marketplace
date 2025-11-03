package com.carbonx.marketcarbon.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private long reportId;
    private String version;          // "logic-no-co2-v1"
    private int dataQualityScore;    // 0..70
    private int dataQualityMax;      // 70
    private int fraudRiskScore;      // 0..30 (càng cao càng rủi ro)
    private int fraudRiskMax;        // 30
    private List<RuleResult> details;
    private List<String> fraudReasons;
}
package com.carbonx.marketcarbon.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {
    private String ruleId;
    private String name;
    private int score;       // điểm rule đạt được
    private int maxScore;    // điểm tối đa rule
    private String message;  // mô tả ngắn
    private String evidence; // bằng chứng rút gọn (text/json)
    private String severity; // INFO/WARN/ERROR
}

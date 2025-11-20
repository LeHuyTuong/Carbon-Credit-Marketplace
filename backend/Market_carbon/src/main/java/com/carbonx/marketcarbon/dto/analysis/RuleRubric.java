package com.carbonx.marketcarbon.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRubric {

    private String ruleId;
    private String name;
    private int maxScore;
    private String description;
    private String scoringGuideline;
    private String evidenceHint;
}

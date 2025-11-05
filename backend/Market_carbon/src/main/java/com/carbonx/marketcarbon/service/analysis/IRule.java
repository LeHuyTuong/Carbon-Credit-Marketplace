package com.carbonx.marketcarbon.service.analysis;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;

public interface IRule {
    String id();
    String name();
    int maxScore();
    RuleResult apply(AnalysisContext ctx);
}

package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

public class EnergyValidRule implements IRule {

    @Override
    public String id() { return "DQ3_ENERGY"; }

    @Override
    public String name() { return "Energy Validity Rule (Greater Than Zero)"; }

    @Override
    public int maxScore() { return 15; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        int total = ctx.getRows().size();
        int nonNumeric = 0;
        int nonPositive = 0;

        for (var row : ctx.getRows()) {
            Object v = row.get("total_energy");
            try {
                double d = Double.parseDouble(String.valueOf(v));
                if (d <= 0) nonPositive++;
            } catch (Exception e) {
                nonNumeric++;
            }
        }

        boolean perfect = (nonNumeric == 0 && nonPositive == 0);

        int score;
        if (perfect) score = 15;
        else if ((total - nonPositive - nonNumeric) >= (total * 0.95)) score = 10;
        else if ((total - nonPositive - nonNumeric) >= (total * 0.80)) score = 5;
        else score = 0;

        String message = perfect
                ? "All energy values are numeric and > 0."
                : "Some rows contain invalid or non-positive energy values.";

        String evidence = String.format(
                "totalRows=%d, nonNumeric=%d, nonPositive=%d",
                total, nonNumeric, nonPositive
        );

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

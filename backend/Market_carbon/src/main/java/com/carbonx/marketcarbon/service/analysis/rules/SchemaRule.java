package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.Set;

public class SchemaRule implements IRule {
    private final Set<String> required;

    public SchemaRule(Set<String> required) {
        this.required = required;
    }

    public String id() { return "DQ1_SCHEMA"; }
    public String name() { return "Schema & Nulls"; }
    public int maxScore() { return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        // Check cột đủ
        boolean hasAll = ctx.getColumns().containsAll(required);
        int nulls = 0;
        if (hasAll) {
            for (var row : ctx.getRows()) {
                for (String col : required) {
                    if (row.get(col) == null) nulls++;
                }
            }
        }
        int score = (hasAll && nulls==0) ? 10 : (hasAll ? 5 : 0);
        String msg = hasAll ? "Columns OK" : "Missing required columns";
        String ev = "required=" + required + ", nullCells=" + nulls;
        return new RuleResult(id(), name(), score, maxScore(), msg, ev, (score==10?"INFO":"ERROR"));
    }
}
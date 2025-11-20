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

    @Override
    public String id() { return "DQ1_SCHEMA"; }

    @Override
    public String name() { return "Schema Validation Rule"; }

    @Override
    public int maxScore() { return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        boolean hasAll = ctx.getColumns().containsAll(required);
        int emptyCells = 0;

        if (hasAll) {
            for (var row : ctx.getRows()) {
                for (String col : required) {
                    if (row.get(col) == null || row.get(col).toString().trim().isEmpty()) {
                        emptyCells++;
                    }
                }
            }
        }

        int score = (hasAll && emptyCells == 0) ? 10 : (hasAll ? 5 : 0);

        String message =
                hasAll && emptyCells == 0
                        ? "All required columns are present and fully populated."
                        : hasAll
                        ? "Some required columns contain empty cells."
                        : "Missing one or more required columns.";

        String evidence = String.format(
                "requiredColumns=%s, actualColumns=%s, emptyCellCount=%d",
                required, ctx.getColumns(), emptyCells
        );

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

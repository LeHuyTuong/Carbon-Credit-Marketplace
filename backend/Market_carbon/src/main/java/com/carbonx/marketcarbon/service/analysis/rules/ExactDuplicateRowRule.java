package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;

public class ExactDuplicateRowRule implements IRule {

    @Override
    public String id() { return "DQ5_DUP_ROW"; }

    @Override
    public String name() { return "Exact Duplicate Row Detection"; }

    @Override
    public int maxScore() { return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        Set<String> signatures = new HashSet<>();
        int duplicates = 0;

        for (var row : ctx.getRows()) {
            String sig = row.toString();
            if (!signatures.add(sig)) duplicates++;
        }

        int score = (duplicates == 0) ? 5 : (duplicates <= 2 ? 3 : 0);

        String message =
                duplicates == 0
                        ? "No exact duplicate rows detected."
                        : "Exact duplicates detected in dataset.";

        String evidence = "duplicateRows=" + duplicates;

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

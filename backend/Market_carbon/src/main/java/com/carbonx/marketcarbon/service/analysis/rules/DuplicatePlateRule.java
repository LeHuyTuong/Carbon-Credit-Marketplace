package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;

public class DuplicatePlateRule implements IRule {

    @Override
    public String id() { return "DQ4_DUP_PLATE"; }

    @Override
    public String name() { return "Duplicate License Plate Detection"; }

    @Override
    public int maxScore() { return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        Set<String> seen = new HashSet<>();
        int dup = 0;

        for (var row : ctx.getRows()) {
            String plate = String.valueOf(row.get("license_plate")).trim();
            if (plate.isEmpty()) continue;
            if (!seen.add(plate)) dup++;
        }

        int score = (dup == 0) ? 10 : (dup <= 2 ? 7 : (dup <= 5 ? 3 : 0));

        String message =
                dup == 0 ? "No duplicate license plates detected." :
                        "Duplicate license plates detected.";

        String evidence = "duplicateCount=" + dup;

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

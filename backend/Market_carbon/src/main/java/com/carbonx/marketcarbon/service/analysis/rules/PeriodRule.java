package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PeriodRule implements IRule {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    @Override
    public String id() { return "DQ2_PERIOD"; }

    @Override
    public String name() { return "Period Consistency Rule"; }

    @Override
    public int maxScore() { return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        Set<String> periods = new HashSet<>();
        boolean formatOK = true;

        for (var row : ctx.getRows()) {
            String p = String.valueOf(row.get("period"));
            if (!DATE_PATTERN.matcher(p).matches()) formatOK = false;
            if (p != null && !p.isEmpty()) periods.add(p);
        }

        int score = (formatOK && periods.size() == 1) ? 10 : (formatOK ? 5 : 0);

        String message =
                formatOK && periods.size() == 1
                        ? "All rows use one valid reporting period."
                        : formatOK
                        ? "Multiple valid periods detected."
                        : "Invalid reporting period format found.";

        String evidence = "periods=" + periods;

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RepeatedRoundedEnergyRule implements IRule {

    private final int scale;

    public RepeatedRoundedEnergyRule(int scale) {
        this.scale = scale;
    }

    @Override
    public String id() { return "DQ8_REPEAT_VALUES"; }

    @Override
    public String name() { return "Repeated Rounded Energy Values Detection"; }

    @Override
    public int maxScore() { return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        Map<String, Integer> freq = new HashMap<>();

        for (var row : ctx.getRows()) {
            try {
                double v = Double.parseDouble(String.valueOf(row.get("total_energy")));
                String key = new BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString();
                freq.merge(key, 1, Integer::sum);
            } catch (Exception ignored) {}
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        int repeatedKeys = (int) entries.stream().filter(e -> e.getValue() > 1).count();
        int score = (repeatedKeys == 0) ? 5 : (repeatedKeys <= 3 ? 3 : 1);

        String top = entries.stream().filter(e -> e.getValue() > 1).limit(5).toList().toString();

        String message =
                repeatedKeys == 0
                        ? "No repeated rounded values detected."
                        : "Repeated rounded values detected in dataset.";

        String evidence = String.format(
                "repeatedKeys=%d, top=%s",
                repeatedKeys, top
        );

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

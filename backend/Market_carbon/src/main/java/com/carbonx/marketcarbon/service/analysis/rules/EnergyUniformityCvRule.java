package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.ArrayList;
import java.util.List;

public class EnergyUniformityCvRule implements IRule {

    private final double threshold;

    public EnergyUniformityCvRule(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String id() { return "DQ7_UNIFORMITY_CV"; }

    @Override
    public String name() { return "Energy Uniformity Rule (Coefficient of Variation)"; }

    @Override
    public int maxScore() { return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        List<Double> vals = new ArrayList<>();
        for (var row : ctx.getRows()) {
            try { vals.add(Double.valueOf(String.valueOf(row.get("total_energy")))); }
            catch (Exception ignored) {}
        }

        if (vals.size() < 2) {
            return new RuleResult(id(), name(), 5, maxScore(),
                    "Not enough data to calculate uniformity (< 2 rows).",
                    "n=" + vals.size());
        }

        double mean = vals.stream().mapToDouble(d -> d).average().orElse(0);
        double var = vals.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum() / (vals.size() - 1);
        double std = Math.sqrt(var);
        double cv = (mean == 0) ? 0 : std / mean;

        boolean suspicious = cv < threshold;

        int score = suspicious ? 2 : 5;

        String message =
                suspicious
                        ? "Values appear too uniform (low CV)."
                        : "Energy values show normal variation.";

        String evidence = String.format(
                "mean=%.6f, std=%.6f, cv=%.6f, threshold=%.6f",
                mean, std, cv, threshold
        );

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }
}

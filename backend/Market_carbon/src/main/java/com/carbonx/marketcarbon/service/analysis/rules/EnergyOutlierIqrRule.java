package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.ArrayList;
import java.util.List;

public class EnergyOutlierIqrRule implements IRule {

    @Override
    public String id() { return "DQ6_OUTLIER_IQR"; }

    @Override
    public String name() { return "Energy Outlier Detection (IQR Method)"; }

    @Override
    public int maxScore() { return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {

        List<Double> vals = new ArrayList<>();
        for (var r : ctx.getRows()) {
            try { vals.add(Double.valueOf(String.valueOf(r.get("total_energy")))); }
            catch (Exception ignored) {}
        }

        if (vals.size() < 4) {
            return new RuleResult(id(), name(), 10, maxScore(),
                    "Not enough data for outlier detection (< 4 rows).",
                    "rows=" + vals.size());
        }

        vals.sort(Double::compareTo);

        double q1 = quantile(vals, 0.25);
        double q3 = quantile(vals, 0.75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        long outliers = vals.stream().filter(v -> v < lower || v > upper).count();

        int score = (outliers == 0) ? 10 : (outliers <= 2 ? 8 : (outliers <= 5 ? 5 : 2));

        String message =
                outliers == 0
                        ? "No strong outliers detected."
                        : "Outliers detected based on the IQR method.";

        String evidence = String.format(
                "q1=%.3f, q3=%.3f, lower=%.3f, upper=%.3f, outliers=%d",
                q1, q3, lower, upper, outliers
        );

        return new RuleResult(id(), name(), score, maxScore(), message, evidence);
    }

    private double quantile(List<Double> sorted, double p) {
        double pos = p * (sorted.size() - 1);
        int i = (int) Math.floor(pos);
        int j = (int) Math.ceil(pos);
        if (i == j) return sorted.get(i);
        double w = pos - i;
        return sorted.get(i) * (1 - w) + sorted.get(j) * w;
    }
}

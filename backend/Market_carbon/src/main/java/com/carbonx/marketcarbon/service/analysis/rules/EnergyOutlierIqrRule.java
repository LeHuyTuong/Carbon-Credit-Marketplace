package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.ArrayList;
import java.util.List;

public class EnergyOutlierIqrRule implements IRule {
    public String id(){ return "DQ6_OUTLIER_IQR"; }
    public String name(){ return "Outlier detection (IQR)"; }
    public int maxScore(){ return 10; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        List<Double> vals = new ArrayList<>();
        for (var r : ctx.getRows()){
            try { vals.add(Double.valueOf(String.valueOf(r.get("total_energy")))); } catch (Exception ignored){}
        }
        if (vals.size()<4) {
            return new RuleResult(id(), name(), 10, maxScore(), "Not enough data (<4)", "n="+vals.size(), "INFO");
        }
        vals.sort(Double::compareTo);
        double q1 = quantile(vals, 0.25);
        double q3 = quantile(vals, 0.75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5*iqr;
        double upper = q3 + 1.5*iqr;
        long out = vals.stream().filter(v -> v<lower || v>upper).count();
        int score = (out==0)? 10 : (out<=2? 8 : (out<=5? 5 : 2));
        String ev = String.format("q1=%.3f,q3=%.3f,lower=%.3f,upper=%.3f,outliers=%d", q1,q3,lower,upper,out);
        return new RuleResult(id(), name(), score, maxScore(), out==0?"No outliers":"Outliers found", ev, out==0?"INFO":"WARN");
    }

    private double quantile(List<Double> sorted, double p){
        if (sorted.isEmpty()) return 0;
        double idx = p * (sorted.size() - 1);
        int i = (int) Math.floor(idx);
        int j = (int) Math.ceil(idx);
        if (i==j) return sorted.get(i);
        double w = idx - i;
        return sorted.get(i)*(1-w) + sorted.get(j)*w;
    }
}

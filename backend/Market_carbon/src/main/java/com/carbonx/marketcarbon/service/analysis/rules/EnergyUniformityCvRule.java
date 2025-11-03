package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.ArrayList;
import java.util.List;

public class EnergyUniformityCvRule implements IRule {
    private final double threshold;
    public EnergyUniformityCvRule(double threshold){ this.threshold = threshold; }

    public String id(){ return "DQ7_UNIFORMITY_CV"; }
    public String name(){ return "Uniformity check (CV)"; }
    public int maxScore(){ return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        List<Double> vals = new ArrayList<>();
        for (var r : ctx.getRows()){
            try { vals.add(Double.valueOf(String.valueOf(r.get("total_energy")))); } catch (Exception ignored){}
        }
        if (vals.size()<2){
            return new RuleResult(id(), name(), 5, maxScore(), "Not enough data (<2)", "n="+vals.size(), "INFO");
        }
        double mean = vals.stream().mapToDouble(d->d).average().orElse(0);
        double var = vals.stream().mapToDouble(d->(d-mean)*(d-mean)).sum()/(vals.size()-1);
        double std = Math.sqrt(var);
        double cv = (mean==0)? 0 : std/mean;
        String ev = String.format("mean=%.6f,std=%.6f,cv=%.6f,threshold=%.6f", mean,std,cv,threshold);
        boolean uniform = (cv < threshold); // quá đồng đều → cảnh báo
        int score = uniform ? 2 : 5;        // đồng đều quá: trừ điểm nhẹ (vì nghi ngờ copy-paste)
        return new RuleResult(id(), name(), score, maxScore(), uniform?"Suspiciously uniform (low CV)":"Dispersion OK", ev, uniform?"WARN":"INFO");
    }
}
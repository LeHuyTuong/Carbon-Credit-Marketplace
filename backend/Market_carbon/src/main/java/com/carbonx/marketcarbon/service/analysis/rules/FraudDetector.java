package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class FraudDetector {

    @Data
    @AllArgsConstructor
    public static class FraudResult {
        private int score;             // 0..30 (cao = rủi ro cao)
        private List<String> reasons;  // giải thích nghi ngờ
    }

    public FraudResult detect(AnalysisContext ctx){
        int score = 0; List<String> reasons = new ArrayList<>();

        // FR1 – Mass duplicate license_plate
        int dup = countDuplicatePlates(ctx.getRows());
        if (dup > 0) { score += Math.min(10, dup*2); reasons.add("Duplicate license_plate rows: "+dup); }

        // FR2 – Copy–paste pattern: repeated rounded energies
        int rep = countRepeatedRoundedEnergies(ctx.getRows(), ctx.getRoundRepeatScale());
        if (rep > 3) { score += 10; reasons.add("Many repeated rounded energy values (keys="+rep+")"); }
        else if (rep > 0) { score += 5; reasons.add("Some repeated rounded energy values (keys="+rep+")"); }

        // FR3 – Uniformity (CV < threshold)
        boolean uniform = isUniformCV(ctx);
        if (uniform) { score += 5; reasons.add("Suspicious uniformity (CV below threshold)"); }

        // FR4 – Period mismatch
        if (!isSingleValidPeriod(ctx)) {
            score += 5; reasons.add("Multiple/invalid periods present");
        }

        score = Math.min(score, 30);
        return new FraudResult(score, reasons);
    }

    private int countDuplicatePlates(List<Map<String,Object>> rows){
        Set<String> seen = new HashSet<>();
        int dup = 0;
        for (var r : rows){
            String plate = r.get("license_plate")==null? "" : String.valueOf(r.get("license_plate")).trim();
            if (plate.isEmpty()) continue;
            if (!seen.add(plate)) dup++;
        }
        return dup;
    }

    private int countRepeatedRoundedEnergies(List<Map<String,Object>> rows, int scale){
        Map<String,Integer> freq = new HashMap<>();
        for (var r : rows){
            try {
                double v = Double.parseDouble(String.valueOf(r.get("total_energy")));
                String key = new BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString();
                freq.merge(key, 1, Integer::sum);
            } catch (Exception ignored){}
        }
        int repeated = 0;
        for (var e : freq.entrySet()) if (e.getValue()>1) repeated++;
        return repeated;
    }

    private boolean isUniformCV(AnalysisContext ctx){
        List<Double> vals = new ArrayList<>();
        for (var r : ctx.getRows()){
            try { vals.add(Double.valueOf(String.valueOf(r.get("total_energy")))); } catch (Exception ignored){}
        }
        if (vals.size()<2) return false;
        double mean = vals.stream().mapToDouble(d->d).average().orElse(0);
        double var = vals.stream().mapToDouble(d->(d-mean)*(d-mean)).sum()/(vals.size()-1);
        double std = Math.sqrt(var);
        double cv = (mean==0)? 0 : std/mean;
        return cv < ctx.getCvUniformityThreshold();
    }

    private boolean isSingleValidPeriod(AnalysisContext ctx){
        var P = java.util.regex.Pattern.compile("^\\d{4}-\\d{2}$");
        Set<String> periods = new HashSet<>();
        boolean fmt = true;
        for (var r : ctx.getRows()){
            String p = String.valueOf(r.get("period"));
            if (!P.matcher(p).matches()) fmt = false;
            if (p!=null && !p.isEmpty()) periods.add(p);
        }
        return fmt && periods.size()==1;
    }
}

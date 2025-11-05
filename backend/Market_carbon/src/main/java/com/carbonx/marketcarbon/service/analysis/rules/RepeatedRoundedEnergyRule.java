package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeatedRoundedEnergyRule implements IRule {
    private final int scale;
    public RepeatedRoundedEnergyRule(int scale){ this.scale = scale; }

    public String id(){ return "DQ8_REPEAT_VALUES"; }
    public String name(){ return "Repeated rounded energies"; }
    public int maxScore(){ return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        Map<String,Integer> freq = new HashMap<>();
        for (var r : ctx.getRows()){
            try {
                double v = Double.parseDouble(String.valueOf(r.get("total_energy")));
                String key = new BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).toPlainString();
                freq.merge(key, 1, Integer::sum);
            } catch (Exception ignored){}
        }
        int repeated = 0;
        List<Map.Entry<String,Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a,b)->Integer.compare(b.getValue(), a.getValue()));
        for (var e : entries) if (e.getValue()>1) repeated++;

        int score = (repeated==0)? 5 : (repeated<=3? 3 : 1);
        String top5 = entries.stream().filter(e->e.getValue()>1).limit(5).toList().toString();
        String ev = "repeatedKeys="+repeated+", top="+top5;
        return new RuleResult(id(), name(), score, maxScore(), repeated==0?"No repeated rounded values":"Repeated rounded values detected", ev, repeated==0?"INFO":"WARN");
    }
}

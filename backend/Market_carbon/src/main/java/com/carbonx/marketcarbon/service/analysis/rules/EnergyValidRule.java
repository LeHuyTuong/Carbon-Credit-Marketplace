package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

public class EnergyValidRule implements IRule {
    public String id(){ return "DQ3_ENERGY"; }
    public String name(){ return "Energy validity (>0 & numeric)"; }
    public int maxScore(){ return 15; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        int total = ctx.getRows().size();
        int invalid = 0, nonNumeric = 0, nonPos = 0;
        for (var r : ctx.getRows()){
            Object v = r.get("total_energy");
            Double d = null;
            try { d = (v==null)? null : Double.valueOf(String.valueOf(v)); }
            catch (Exception ex){ nonNumeric++; }
            if (d==null) { nonNumeric++; }
            else if (d <= 0) { nonPos++; invalid++; }
        }
        int ok = total - Math.max(invalid,0) - nonNumeric;
        int score = (nonNumeric==0 && nonPos==0)? 15 : (ok>= total*0.95? 10 : (ok>= total*0.8? 5 : 0));
        String ev = "total="+total+", nonNumeric="+nonNumeric+", nonPositive="+nonPos;
        return new RuleResult(id(), name(), score, maxScore(), (score==15?"OK":"Issues found"), ev, score==15?"INFO":"WARN");
    }
}

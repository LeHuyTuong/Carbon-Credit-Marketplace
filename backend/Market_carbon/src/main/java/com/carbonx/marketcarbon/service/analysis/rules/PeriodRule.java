package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PeriodRule implements IRule {
    private static final Pattern P = Pattern.compile("^\\d{4}-\\d{2}$");

    public String id(){ return "DQ2_PERIOD"; }
    public String name(){ return "Period format & single period"; }
    public int maxScore(){ return 10; }

    public RuleResult apply(AnalysisContext ctx) {
        Set<String> periods = new HashSet<>();
        boolean formatOk = true;
        for (var r : ctx.getRows()){
            Object p = r.get("period");
            String s = (p==null)? "": String.valueOf(p);
            if (!P.matcher(s).matches()) formatOk = false;
            if (!s.isEmpty()) periods.add(s);
        }
        boolean single = periods.size()==1;
        int score = (formatOk && single) ? 10 : (formatOk ? 5 : 0);
        String msg = (formatOk && single)? "One valid period" : (formatOk? "Multiple periods":"Invalid format");
        return new RuleResult(id(), name(), score, maxScore(), msg, "periods="+periods, score==10?"INFO":"WARN");
    }
}
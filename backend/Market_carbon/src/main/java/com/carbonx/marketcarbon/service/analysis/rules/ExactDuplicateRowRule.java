package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;

public class ExactDuplicateRowRule implements IRule {
    public String id(){ return "DQ5_DUP_ROW"; }
    public String name(){ return "Exact duplicate rows"; }
    public int maxScore(){ return 5; }

    @Override
    public RuleResult apply(AnalysisContext ctx) {
        Set<String> sigs = new HashSet<>();
        int dup = 0;
        for (var r : ctx.getRows()){
            String sig = r.toString();
            if (!sigs.add(sig)) dup++;
        }
        int score = (dup==0)? 5 : (dup<=2? 3 : 0);
        return new RuleResult(id(), name(), score, maxScore(), dup==0?"No duplicate rows":"Duplicate rows found", "dupRows="+dup, dup==0?"INFO":"WARN");
    }
}

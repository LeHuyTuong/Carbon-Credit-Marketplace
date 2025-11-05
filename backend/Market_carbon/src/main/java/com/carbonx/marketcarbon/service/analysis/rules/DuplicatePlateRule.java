package com.carbonx.marketcarbon.service.analysis.rules;

import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.service.analysis.AnalysisContext;
import com.carbonx.marketcarbon.service.analysis.IRule;

import java.util.HashSet;
import java.util.Set;

public class DuplicatePlateRule implements IRule {
    public String id(){ return "DQ4_DUP_PLATE"; }
    public String name(){ return "Duplicate license plates"; }
    public int maxScore(){ return 10; }

    public RuleResult apply(AnalysisContext ctx) {
        Set<String> seen = new HashSet<>();
        int dup = 0;
        for (var r : ctx.getRows()){
            String plate = r.get("license_plate")==null? "": String.valueOf(r.get("license_plate")).trim();
            if (plate.isEmpty()) continue;
            if (!seen.add(plate)) dup++;
        }
        int score = (dup==0)? 10 : (dup<=2? 7 : (dup<=5? 3 : 0));
        String ev = "duplicates="+dup;
        return new RuleResult(id(), name(), score, maxScore(), (dup==0?"No duplicates":"Found duplicates"), ev, dup==0?"INFO":"WARN");
    }
}

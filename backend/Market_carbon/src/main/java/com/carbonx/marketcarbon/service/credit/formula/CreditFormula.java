package com.carbonx.marketcarbon.service.credit.formula;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.Project;

public interface CreditFormula {
    CreditComputationResult compute(EmissionReport report, Project project);
}

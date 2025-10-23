package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;

import java.math.BigDecimal;
import java.util.List;

public interface AiScoringService {
    record AiScoreResult(BigDecimal score, String notes, String version) {}
    AiScoreResult suggestScore(EmissionReport report, List<EmissionReportDetail> details);
}

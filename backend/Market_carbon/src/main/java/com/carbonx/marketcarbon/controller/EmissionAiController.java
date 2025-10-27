package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.response.AiEvaluationResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.service.EmissionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class EmissionAiController {

    private final EmissionReportService emissionReportService;

    @PostMapping("/{id}/ai-score")
    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    public AiEvaluationResponse aiScore(@PathVariable("id") Long reportId) {
        EmissionReportResponse updated = emissionReportService.aiSuggestScore(reportId);
        return new AiEvaluationResponse(
                updated.getAiPreScore() != null ? updated.getAiPreScore().doubleValue() : null,
                updated.getAiVersion(),
                updated.getAiPreNotes()
        );
    }
}

package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.service.EmissionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class EmissionAiController {

    private final EmissionReportService emissionReportService;

    @PostMapping("/{id}/ai-score")
    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    public EmissionReportResponse aiScore(@PathVariable("id") Long reportId) {
        return emissionReportService.aiSuggestScore(reportId);
    }
}

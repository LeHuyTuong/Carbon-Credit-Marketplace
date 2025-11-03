package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.analysis.AnalysisResult;
import com.carbonx.marketcarbon.service.analysis.ReportAnalysisService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportAnalysisController {

    private final ReportAnalysisService analysisService;

    @PreAuthorize("hasAnyRole('ADMIN','CVA','COMPANY')")
    @Operation(summary = "Analyze report (no-CO2 profile)", description = "Data quality + fraud-lite without CO₂/factor checks")
    @PostMapping("/{id}/analyze")
    public ResponseEntity<TuongCommonResponse<AnalysisResult>> analyze(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "true") boolean persist,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        AnalysisResult result = analysisService.analyzeNoCo2(id, persist);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<AnalysisResult> response = new TuongCommonResponse<>(trace, now, rs, result);
        return ResponseEntity.ok(response);
    }
}

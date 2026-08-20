package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.analysis.AnalysisResult;
import com.carbonx.marketcarbon.dto.analysis.RuleResult;
import com.carbonx.marketcarbon.dto.analysis.RuleRubric;
import com.carbonx.marketcarbon.service.analysis.ReportAnalysisService;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportAnalysisController {

    private final ReportAnalysisService analysisService;

    @PreAuthorize("hasAnyRole('ADMIN','CVA','COMPANY')")
    @Operation(summary = "Analyze report (no-CO2 profile)", description = "Data quality + fraud-lite without CO₂/factor checks")
    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<AnalysisResult>> analyze(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "true") boolean persist,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        AnalysisResult result = analysisService.analyzeNoCo2(id, persist);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<AnalysisResult> response = new ApiResponse<>(trace, now, rs, result);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CVA','COMPANY')")
    @Operation(
            summary = "Get rule-by-rule scoring breakdown",
            description = "Returns the detailed scoring for each validation rule"
    )
    @GetMapping("/{id}/analysis/rules")
    public ResponseEntity<ApiResponse<List<RuleResult>>> getRuleDetails(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        AnalysisResult ar = analysisService.analyzeNoCo2(id, false);

        ApiResponse<List<RuleResult>> response =
                new ApiResponse<>(
                        trace,
                        now,
                        new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage()),
                        ar.getDetails()
                );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CVA','COMPANY')")
    @Operation(
            summary = "Get rule rubric template",
            description = "Returns the list of rules with scoring guidelines and evidence hints for manual or AI-based evaluation."
    )
    @GetMapping("/analysis/rules/rubric")
    public ResponseEntity<ApiResponse<List<RuleRubric>>> getRuleRubricTemplate(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null)
                ? requestDateTime
                : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<RuleRubric> data = analysisService.getRuleRubrics();

        ResponseStatus status = new ResponseStatus(
                StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage()
        );

        ApiResponse<List<RuleRubric>> response =
                new ApiResponse<>(trace, now, status, data);

        return ResponseEntity.ok(response);
    }


}

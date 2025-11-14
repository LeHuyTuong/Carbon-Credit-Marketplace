package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.analysis.AnalysisResult;
import com.carbonx.marketcarbon.dto.analysis.RuleResult;
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

    @PreAuthorize("hasAnyRole('ADMIN','CVA','COMPANY')")
    @Operation(
            summary = "Get rule-by-rule scoring breakdown",
            description = "Returns the detailed scoring for each validation rule"
    )
    @GetMapping("/{id}/analysis/rules")
    public ResponseEntity<TuongCommonResponse<List<RuleResult>>> getRuleDetails(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        AnalysisResult ar = analysisService.analyzeNoCo2(id, false);

        TuongResponseStatus status = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage()
        );

        TuongCommonResponse<List<RuleResult>> response =
                new TuongCommonResponse<>(trace, now, status, ar.getDetails());

        return ResponseEntity.ok(response);
    }


    // GET FRAUD-ONLY – chỉ trả về Fraud score + lý do
    @PreAuthorize("hasAnyRole('ADMIN','CVA')")
    @Operation(
            summary = "Get fraud-lite result",
            description = "Returns fraud score and fraud reasons only (no DQ scoring)"
    )
    @GetMapping("/{id}/analysis/fraud")
    public ResponseEntity<TuongCommonResponse<Object>> getFraudOnly(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        AnalysisResult ar = analysisService.analyzeNoCo2(id, false);

        Map<String, Object> out = new HashMap<>();
        out.put("fraudRiskScore", ar.getFraudRiskScore());
        out.put("fraudRiskMax", ar.getFraudRiskMax());
        out.put("reasons", ar.getFraudReasons());

        TuongResponseStatus status = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage()
        );

        TuongCommonResponse<Object> response =
                new TuongCommonResponse<>(trace, now, status, out);

        return ResponseEntity.ok(response);
    }

}

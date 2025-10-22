package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import com.carbonx.marketcarbon.service.CreditIssuanceService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditIssuanceController {

    private final CreditIssuanceService creditIssuanceService;

    @Operation(summary = "Issue carbon credits for an approved emission report (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/issue")
    public ResponseEntity<TuongCommonResponse<CreditBatchResponse>> issueCredits(
            @RequestParam("reportId") @NotNull Long reportId,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String reqTrace = (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
        String now = (dateTime != null && !dateTime.isBlank()) ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CreditBatchResponse data = creditIssuanceService.issueForReport(reportId);

        TuongResponseStatus rs = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                "Credits issued successfully"
        );
        return ResponseEntity.ok(new TuongCommonResponse<>(reqTrace, now, rs, data));
    }


    @Operation(summary = "List all issued credit batches (paginated)")
    @GetMapping("/batches")
    public ResponseEntity<TuongCommonResponse<Page<CreditBatchResponse>>> listAllBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String reqTrace = (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
        String now = (dateTime != null && !dateTime.isBlank()) ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<CreditBatchResponse> data = creditIssuanceService.listAllBatches(pageable);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(reqTrace, now, rs, data));
    }

    @Operation(summary = "Get details of a credit batch by ID")
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<TuongCommonResponse<CreditBatchResponse>> getBatchById(
            @PathVariable("batchId") Long batchId,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String reqTrace = (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
        String now = (dateTime != null && !dateTime.isBlank()) ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CreditBatchResponse data = creditIssuanceService.getBatchById(batchId);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Batch details retrieved");
        return ResponseEntity.ok(new TuongCommonResponse<>(reqTrace, now, rs, data));
    }
}
package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.RetireBatchRequest;
import com.carbonx.marketcarbon.dto.request.RetireCreditRequest;
import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse;
import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import com.carbonx.marketcarbon.dto.response.RetirableBatchResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.service.CreditQuery;
import com.carbonx.marketcarbon.service.MyCreditService;
import com.carbonx.marketcarbon.service.MyCreditInventoryService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/my/credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class MyCreditController {

    private final MyCreditService creditService;
    private final MyCreditInventoryService inventoryService;

    @Operation(summary = "[COMPANY] Get My Carbon Credits",
            description = "Returns paginated list of carbon credits owned by current company, optionally filtered by project or status.")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<CarbonCreditResponse>>> listMyCredits(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer vintageYear,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CreditStatus creditStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                creditStatus = CreditStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        var query = new CreditQuery(projectId, vintageYear, creditStatus);

        List<CarbonCreditResponse> result = creditService.listMyCredits(query);

        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get my credits successfully"),
                result
        );

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "[COMPANY] Get Credit by ID",
            description = "Retrieve details of a specific carbon credit owned by the current company.")
    @GetMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<CarbonCreditResponse>> getCreditById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CarbonCreditResponse data = creditService.getMyCreditById(id);

        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get credit details successfully"),
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Get My Credit Batches",
            description = "Returns paginated list of credit batches belonging to the company.")
    @GetMapping("/batches")
    public ResponseEntity<TuongCommonResponse<Page<CreditBatchLiteResponse>>> listMyBatches(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer vintageYear,
            Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<CreditBatchLiteResponse> result = creditService.listMyBatches(projectId, vintageYear, pageable);

        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get my credit batches successfully"),
                result
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Credit Inventory Summary",
            description = "Returns aggregated totals by status, project, and vintage year.")
    @GetMapping("/summary")
    public ResponseEntity<TuongCommonResponse<CreditInventorySummaryResponse>> getInventorySummary(
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        var data = inventoryService.getMyInventorySummary();
        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get credit inventory summary successfully"),
                data
        );
        return ResponseEntity.ok(response);
    }

    //  5. Available Balance
    @Operation(summary = "[COMPANY] Get Available Credit Balance",
            description = "Returns total available credits (SUM of amount with status AVAILABLE).")
    @GetMapping("/balance")
    public ResponseEntity<TuongCommonResponse<Long>> getAvailableBalance(
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        long balance = inventoryService.getMyAvailableBalance();
        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get available balance successfully"),
                balance
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Get Credits in a Batch",
            description = "Returns list of carbon credits owned by company that belong to a specific batch.")
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<TuongCommonResponse<List<CarbonCreditResponse>>> getCreditsInBatch(
            @PathVariable("batchId") Long batchId,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        // Gọi service để lấy danh sách credit thuộc batch này và thuộc công ty hiện tại
        List<CarbonCreditResponse> credits = creditService.getMyCreditsByBatchId(batchId);

        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get credits by batch successfully"),
                credits
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Get credits eligible for retirement",
            description = "Returns credits owned by company that are not expired, retired, or listed.")
    @GetMapping("/retirable")
    public ResponseEntity<TuongCommonResponse<List<CarbonCreditResponse>>> listRetirableCredits(
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<CarbonCreditResponse> data = creditService.getMyRetirableCredits();
        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get retirable credits successfully"),
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Retire a carbon credit block",
            description = "Retires a quantity from the specified carbon credit. When the quantity reaches zero, the credit is marked as RETIRED.")
    @PostMapping("/{id}/retire")
    public ResponseEntity<TuongCommonResponse<List<CarbonCreditResponse> >> retireCredit(
            @Valid @RequestBody TuongCommonRequest<RetireBatchRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<CarbonCreditResponse> data = creditService.retireCreditsFromBatch (request.getData());

        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Retire carbon credit successfully"),
                data
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[COMPANY] Get Batches eligible for retirement",
            description = "Returns batches owned by company that contain credits eligible for retirement, grouped by batch.")
    @GetMapping("/retirable-batches")
    public ResponseEntity<TuongCommonResponse<List<RetirableBatchResponse>>> listRetirableCreditsByBatch(
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateTime
    ) {
        String traceId = trace != null ? trace : UUID.randomUUID().toString();
        String now = dateTime != null ? dateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<RetirableBatchResponse> data = creditService.getMyRetirableCreditsBatch();
        var response = new TuongCommonResponse<>(
                traceId,
                now,
                new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Get retirable batches successfully"),
                data
        );
        return ResponseEntity.ok(response);
    }


}

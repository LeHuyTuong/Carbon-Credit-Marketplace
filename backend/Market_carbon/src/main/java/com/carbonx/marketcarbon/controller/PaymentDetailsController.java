package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.service.impl.PaymentDetailsServiceImpl;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/paymentDetails")
@RequiredArgsConstructor
public class PaymentDetailsController {
    private final PaymentDetailsServiceImpl paymentDetailsService;

    @Operation(summary = "Create a method to withdrawal" , description = "Add bankAccount to withdrawal")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<PaymentDetails>> createPaymentDetails(
            @Valid  @RequestBody TuongCommonRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentDetails paymentDetails = paymentDetailsService.addPaymentDetails(req.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentDetails> res = new TuongCommonResponse<>(trace, now , rs , paymentDetails);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Update method to withdrawal" , description = "Update bankAccount to withdrawal")
    @PutMapping
    public ResponseEntity<TuongCommonResponse<PaymentDetails>> updatePaymentDetails(
            @Valid  @RequestBody TuongCommonRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PaymentDetails paymentDetails = paymentDetailsService.updatePaymentDetails(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentDetails> res = new TuongCommonResponse<>(trace, now , rs , paymentDetails);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Delete a method to withdrawal" , description = "Delete bankAccount to withdrawal")
    @DeleteMapping
    public ResponseEntity<TuongCommonResponse<PaymentDetails>> deletePaymentDetails(
            @Valid  @RequestBody TuongCommonRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        paymentDetailsService.deletePaymentDetails(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentDetails> res = new TuongCommonResponse<>(trace, now , rs , null);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "View bankAccount", description = "API to view all of bankAccount to withdrawl")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<PaymentDetails>> getPaymentDetails(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentDetails paymentDetails = paymentDetailsService.getUserPaymentDetails();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentDetails> res = new TuongCommonResponse<>(trace,now,rs,paymentDetails);
        return ResponseEntity.ok(res);
    }
}

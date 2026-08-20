package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.service.impl.PaymentDetailsServiceImpl;
import com.carbonx.marketcarbon.utils.common.ApiRequest;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
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
    public ResponseEntity<ApiResponse<PaymentDetails>> createPaymentDetails(
            @Valid  @RequestBody ApiRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentDetails paymentDetails = paymentDetailsService.addPaymentDetails(req.getData());
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<PaymentDetails> res = new ApiResponse<>(trace, now , rs , paymentDetails);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Update method to withdrawal" , description = "Update bankAccount to withdrawal")
    @PutMapping
    public ResponseEntity<ApiResponse<PaymentDetails>> updatePaymentDetails(
            @Valid  @RequestBody ApiRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PaymentDetails paymentDetails = paymentDetailsService.updatePaymentDetails(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<PaymentDetails> res = new ApiResponse<>(trace, now , rs , paymentDetails);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Delete a method to withdrawal" , description = "Delete bankAccount to withdrawal")
    @DeleteMapping
    public ResponseEntity<ApiResponse<PaymentDetails>> deletePaymentDetails(
            @Valid  @RequestBody ApiRequest<PaymentDetailsRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        paymentDetailsService.deletePaymentDetails(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<PaymentDetails> res = new ApiResponse<>(trace, now , rs , null);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "View bankAccount", description = "API to view all of bankAccount to withdrawl")
    @GetMapping
    public ResponseEntity<ApiResponse<PaymentDetails>> getPaymentDetails(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentDetails paymentDetails = paymentDetailsService.getUserPaymentDetails();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<PaymentDetails> res = new ApiResponse<>(trace,now,rs,paymentDetails);
        return ResponseEntity.ok(res);
    }
}

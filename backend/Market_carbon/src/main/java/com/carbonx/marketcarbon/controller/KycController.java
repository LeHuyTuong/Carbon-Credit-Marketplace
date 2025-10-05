package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.dto.request.KycRequest;
import com.carbonx.marketcarbon.service.KycService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @Operation(summary = "Create Vehicle" , description = "API Create Vehicle")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<Long>> create(
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long userID = kycService.create(req.getData());

            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, userID);
            return ResponseEntity.ok(response);

    }

    @Operation(summary = "Update KYC ", description = "Update KYC for user ")
    @PutMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Long>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long updateUsedID = kycService.update(id, req.getData());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updateUsedID);
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get KYC of user ", description = "User get their KYC ")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<KycProfile>> getByUser(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        KycProfile kyc = kycService.getByUserId();
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<KycProfile> response = new TuongCommonResponse<>(trace, now, rs, kyc);
            return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get all KYC of user ", description = "Admin get all KYC")
    @GetMapping("/listKYC")
    public ResponseEntity<TuongCommonResponse<List<KycResponse>>> getAllKYCByAdmin(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<KycResponse> kyc = kycService.getAllKYC();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycResponse>> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }


}

package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.domain.StatusCode;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.ApiResponse;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    public ResponseEntity<TuongCommonResponse<Long>> create(
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long userID = kycService.update(req.getData().getUserId(), req.getData());

        try {
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, userID);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.BAD_REQUEST.getCode(),
                    StatusCode.BAD_REQUEST.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, userID);
            return ResponseEntity.ok(response);
        }
    }

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
        try {
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updateUsedID);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updateUsedID);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<TuongCommonResponse<KycProfile>> getByUser(
            @PathVariable(name = "userId") @Valid Long userId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        KycProfile kyc = kycService.getByUserId(userId);
        try {
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<KycProfile> response = new TuongCommonResponse<>(trace, now, rs, kyc);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<KycProfile> response = new TuongCommonResponse<>(trace, now, rs, kyc);
            return ResponseEntity.ok(response);
        }
    }


}

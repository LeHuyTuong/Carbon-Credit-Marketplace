package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.KycCompanyRequest;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.EVOwner;
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
import org.springframework.validation.annotation.Validated;
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
    @PostMapping("/user")
    public ResponseEntity<TuongCommonResponse<Long>> create(
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long userID = kycService.createUser(req.getData());

            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, userID);
            return ResponseEntity.ok(response);

    }

    @Operation(summary = "Update KYC ", description = "Update KYC for user ")
    @PutMapping("/user")
    public ResponseEntity<TuongCommonResponse<Long>> update(
            @Validated(KycRequest.Update.class) @RequestBody  TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long updateUsedID = kycService.updateUser( req.getData());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updateUsedID);
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get KYC of user ", description = "User get their KYC ")
    @GetMapping("/user")
    public ResponseEntity<TuongCommonResponse<EVOwner>> getByUser(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        EVOwner kyc = kycService.getByUserId();
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<EVOwner> response = new TuongCommonResponse<>(trace, now, rs, kyc);
            return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get all KYC of user ", description = "Admin get all KYC")
    @GetMapping("/user/listKYC")
    public ResponseEntity<TuongCommonResponse<List<KycResponse>>> getAllKYCByAdmin(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<KycResponse> kyc = kycService.getAllKYCUser();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycResponse>> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Create KYC for Company", description = "API create KYC Company")
    @PostMapping("/company")
    public ResponseEntity<TuongCommonResponse<Long>> createKycCompany(
            @Validated(KycCompanyRequest.Create.class) @RequestBody TuongCommonRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long kyc = kycService.createCompany(req.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Update KYC for Company", description = "API update KYC Company")
    @PutMapping("/company")
    public ResponseEntity<TuongCommonResponse<Long>> updateKycCompany(
            @Validated(KycCompanyRequest.Update.class) @RequestBody TuongCommonRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long kyc = kycService.updateCompany(req.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "KYC company", description = "API get info company")
    @GetMapping("/company")
    public ResponseEntity<TuongCommonResponse<KycCompanyResponse>> getKycCompany(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        KycCompanyResponse kyc = kycService.getByCompanyId();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<KycCompanyResponse> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "KYC company get all by admin", description = "API get info company by admin ")
    @GetMapping("/company/listKYCCompanmy")
    public ResponseEntity<TuongCommonResponse<List<KycCompanyResponse>>> getAllKycCompany(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<KycCompanyResponse> kyc = kycService.getAllKYCCompany();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycCompanyResponse>> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

}

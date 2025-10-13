package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.*;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
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


    @Operation(summary = "Create KYC for User", description = "Create KYC profile for the current user")
    @PostMapping("/user")
    public ResponseEntity<TuongCommonResponse<Long>> createUser(
            @Valid @RequestBody TuongCommonRequest<@Valid KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long userId = kycService.createUser(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for User", description = "Update KYC profile for the current user")
    @PutMapping("/user")
    public ResponseEntity<TuongCommonResponse<Long>> updateUser(
            @Validated(KycRequest.Update.class) @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long updatedUserId = kycService.updateUser(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updatedUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get KYC of current User", description = "Get KYC profile of the current user")
    @GetMapping("/user")
    public ResponseEntity<TuongCommonResponse<EVOwner>> getUserKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        EVOwner kyc = kycService.getByUserId();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<EVOwner> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all User KYC", description = "Admin get all user KYC profiles")
    @GetMapping("/user/listKYC")
    public ResponseEntity<TuongCommonResponse<List<KycResponse>>> listUserKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycResponse> list = kycService.getAllKYCUser();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycResponse>> response = new TuongCommonResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Create KYC for Company", description = "Create KYC profile for the company of current user")
    @PostMapping("/company")
    public ResponseEntity<TuongCommonResponse<Long>> createCompany(
            @Validated(KycCompanyRequest.Create.class) @RequestBody TuongCommonRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createCompany(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for Company", description = "Update KYC profile for the company of current user")
    @PutMapping("/company")
    public ResponseEntity<TuongCommonResponse<Long>> updateCompany(
            @Validated(KycCompanyRequest.Update.class) @RequestBody TuongCommonRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateCompany(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Company KYC", description = "Get KYC profile of the current user's company")
    @GetMapping("/company")
    public ResponseEntity<TuongCommonResponse<Company>> getCompanyKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Company kyc = kycService.getByCompanyId();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Company> response = new TuongCommonResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all Company KYC", description = "Admin get all company KYC profiles")
    @GetMapping("/company/listKYCCompany")
    public ResponseEntity<TuongCommonResponse<List<KycCompanyResponse>>> listCompanyKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycCompanyResponse> list = kycService.getAllKYCCompany();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycCompanyResponse>> response = new TuongCommonResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Create KYC for CVA", description = "Create KYC profile for CVA (current user)")
    @PostMapping("/cva/create")
    public ResponseEntity<TuongCommonResponse<Long>> createCva(
            @Valid @RequestBody TuongCommonRequest<@Valid KycCvaRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createCva(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for CVA", description = "Update KYC profile for CVA (current user)")
    @PutMapping("/cva")
    public ResponseEntity<TuongCommonResponse<Long>> updateCva(
            @Valid @RequestBody TuongCommonRequest<@Valid KycCvaRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateCva(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get CVA KYC (me)", description = "Get KYC profile of the current CVA user")
    @GetMapping("/cva")
    public ResponseEntity<TuongCommonResponse<KycCvaResponse>> getCvaProfile(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        KycCvaResponse data = kycService.getCvaProfile();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<KycCvaResponse> response = new TuongCommonResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all CVA KYC", description = "Admin get all CVA KYC profiles")
    @GetMapping("/cva/list")
    public ResponseEntity<TuongCommonResponse<List<KycCvaResponse>>> listCvaProfiles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycCvaResponse> list = kycService.getAllCvaProfiles();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<KycCvaResponse>> response = new TuongCommonResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create KYC for Admin", description = "Create KYC profile for Admin (current user)")
    @PostMapping("/admin")
    public ResponseEntity<TuongCommonResponse<Long>> createAdmin(
            @Valid @RequestBody TuongCommonRequest<@Valid KycAdminRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createAdmin(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for Admin", description = "Update KYC profile for Admin (current user)")
    @PutMapping("/admin")
    public ResponseEntity<TuongCommonResponse<Long>> updateAdmin(
            @Valid @RequestBody TuongCommonRequest<@Valid KycAdminRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateAdmin(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Admin KYC (me)", description = "Get KYC profile of the current Admin user")
    @GetMapping("/admin")
    public ResponseEntity<TuongCommonResponse<KycAdminResponse>> getAdminProfile(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        KycAdminResponse data = kycService.getAdminProfile();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<KycAdminResponse> response = new TuongCommonResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

}

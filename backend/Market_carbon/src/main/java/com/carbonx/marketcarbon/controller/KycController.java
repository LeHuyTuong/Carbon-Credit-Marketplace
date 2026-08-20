package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.KycAdminResponse;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycCvaResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.service.KycService;
import com.carbonx.marketcarbon.utils.common.ApiRequest;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ApiResponse<Long>> createUser(
            @Valid @RequestBody ApiRequest<@Valid KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long userId = kycService.createUser(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for User", description = "Update KYC profile for the current user")
    @PutMapping("/user")
    public ResponseEntity<ApiResponse<Long>> updateUser(
            @Valid @Validated(KycRequest.Update.class) @RequestBody ApiRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long updatedUserId = kycService.updateUser(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, updatedUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get KYC of current User", description = "Get KYC profile of the current user")
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<EVOwner>> getUserKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        EVOwner kyc = kycService.getByUserId();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<EVOwner> response = new ApiResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all User KYC", description = "Admin get all user KYC profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @GetMapping("/user/listKYC")
    public ResponseEntity<ApiResponse<List<KycResponse>>> listUserKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycResponse> list = kycService.getAllKYCUser();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<KycResponse>> response = new ApiResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all KYC users", description = "View list of all KYC user profiles (for ADMIN or CVA)")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @GetMapping("/listEvowner")
    public ResponseEntity<ApiResponse<List<KycResponse>>> getAllKYCUsers(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        // Gọi service để lấy danh sách
        List<KycResponse> kycList = kycService.getAllKYCUser();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<KycResponse>> response = new ApiResponse<>(trace, now, rs, kycList);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Create KYC for Company", description = "Create KYC profile for the company of current user")
    @PostMapping("/company")
    public ResponseEntity<ApiResponse<Long>> createCompany(
            @Valid @Validated(KycCompanyRequest.Create.class) @RequestBody ApiRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createCompany(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @Operation(summary = "Update KYC for Company", description = "Update KYC profile for the company of current user")
    @PutMapping("/company")
    public ResponseEntity<ApiResponse<Long>> updateCompany(
            @Valid @Validated(KycCompanyRequest.Update.class) @RequestBody ApiRequest<KycCompanyRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateCompany(req.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get Company KYC by ID (Admin/CVA)",
            description = "Allow ADMIN or CVA to retrieve KYC information of any company by its ID"
    )
    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    public ResponseEntity<KycCompanyResponse> getCompanyInfoById(@PathVariable Long companyId) {
        return ResponseEntity.ok(kycService.getByCompanyIdForAdminOrCva(companyId));
    }

    @Operation(summary = "Get Company KYC", description = "Get KYC profile of the current user's company")
    @GetMapping("/company")
    public ResponseEntity<ApiResponse<KycCompanyResponse>> getKycCompany(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        KycCompanyResponse kyc = kycService.getByCompanyId();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<KycCompanyResponse> response = new ApiResponse<>(trace, now, rs, kyc);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all Company KYC", description = "Admin get all company KYC profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @GetMapping("/company/listKYCCompany")
    public ResponseEntity<ApiResponse<List<KycCompanyResponse>>> listCompanyKyc(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycCompanyResponse> list = kycService.getAllKYCCompany();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<KycCompanyResponse>> response = new ApiResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Create KYC for CVA", description = "Create KYC profile for CVA (current user)")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @PostMapping(value = "/cva/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createCva(
            @Valid @ModelAttribute KycCvaRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createCva(req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Update KYC for CVA", description = "Update KYC profile for CVA (current user)")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @PutMapping(value = "/cva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateCva(
            @Valid @ModelAttribute KycCvaRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateCva(req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get CVA KYC (me)", description = "Get KYC profile of the current CVA user")
    @GetMapping("/cva")
    public ResponseEntity<ApiResponse<KycCvaResponse>> getCvaProfile(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        KycCvaResponse data = kycService.getCvaProfile();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<KycCvaResponse> response = new ApiResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all CVA KYC", description = "Admin get all CVA KYC profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    @GetMapping("/cva/list")
    public ResponseEntity<ApiResponse<List<KycCvaResponse>>> listCvaProfiles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<KycCvaResponse> list = kycService.getAllCvaProfiles();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<KycCvaResponse>> response = new ApiResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create KYC for Admin", description = "Create KYC profile for Admin (current user)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createAdmin(
            @ModelAttribute @Valid KycAdminRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createAdmin(req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Admin KYC created successfully");
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update KYC for Admin", description = "Update KYC profile for Admin (current user)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateAdmin(
            @ModelAttribute @Valid KycAdminRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.updateAdmin(req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Admin KYC updated successfully");
        ApiResponse<Long> response = new ApiResponse<>(trace, now, rs, id);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get Admin KYC (me)", description = "Get KYC profile of the current Admin user")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<KycAdminResponse>> getAdminProfile(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        KycAdminResponse data = kycService.getAdminProfile();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<KycAdminResponse> response = new ApiResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create KYC for EV Owner", description = "EV Owner submits identification information for verification")
    @PostMapping("/kycEv")
    public ResponseEntity<ApiResponse<Long>> createKyc(
            @Valid @RequestBody KycEvOwnerRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long id = kycService.createKycEVOwner(req); // gọi logic cũ trong service
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "KYC created successfully");

        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, id));
    }

}

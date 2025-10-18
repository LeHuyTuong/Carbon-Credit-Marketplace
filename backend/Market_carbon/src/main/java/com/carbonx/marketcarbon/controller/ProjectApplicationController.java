package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProjectApplicationRequest;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import com.carbonx.marketcarbon.service.ProjectApplicationService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-applications")
@RequiredArgsConstructor
public class ProjectApplicationController {

    private final ProjectApplicationService projectApplicationService;


    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Submit application to join a project (Company only)")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<ProjectApplicationResponse>> submitApplication(
            @Valid @RequestBody TuongCommonRequest<ProjectApplicationRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectApplicationResponse data = projectApplicationService.submit(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Application submitted successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "List all my submitted applications (Company only)")
    @GetMapping("/my")
    public ResponseEntity<TuongCommonResponse<List<ProjectApplicationResponse>>> listMyApplications(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectApplicationResponse> data = projectApplicationService.listMyApplications(status);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }


    @PreAuthorize("hasRole('CVA')")
    @Operation(summary = "CVA approve or reject application")
    @PutMapping("/{applicationId}/cva-decision")
    public ResponseEntity<TuongCommonResponse<ProjectApplicationResponse>> cvaDecision(
            @PathVariable Long applicationId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String note,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectApplicationResponse data = projectApplicationService.cvaDecision(applicationId, approved, note);

        TuongResponseStatus rs = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                approved ? "CVA approved application" : "CVA rejected application"
        );

        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin final decision (approve or reject)")
    @PutMapping("/{applicationId}/admin-decision")
    public ResponseEntity<TuongCommonResponse<ProjectApplicationResponse>> adminFinalDecision(
            @PathVariable Long applicationId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String note,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectApplicationResponse data = projectApplicationService.adminFinalDecision(applicationId, approved, note);

        TuongResponseStatus rs = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                approved ? "Admin approved application and issued carbon credits"
                        : "Admin rejected application"
        );

        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get all applications (for Admin debug or audit)")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<ProjectApplicationResponse>>> listAll(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectApplicationResponse> data = projectApplicationService.listAll();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get application by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<ProjectApplicationResponse>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectApplicationResponse data = projectApplicationService.getById(id);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @PreAuthorize("hasRole('CVA')")
    @Operation(summary = "List applications awaiting CVA review")
    @GetMapping("/pending-cva")
    public ResponseEntity<TuongCommonResponse<List<ProjectApplicationResponse>>> listPendingForCva(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectApplicationResponse> data = projectApplicationService.listPendingForCva();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }
}

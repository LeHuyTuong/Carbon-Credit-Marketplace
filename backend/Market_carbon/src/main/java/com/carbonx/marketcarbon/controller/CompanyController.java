package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.CompanyService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/company/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyRole('COMPANY')")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "List available base projects for company")
    @GetMapping("/base-choices")
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse>>> listBaseChoices(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = companyService.listBaseProjectChoices(pageable);
        var rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Fetched base project choices");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Apply to a base project (create draft application)")
    @PostMapping("/apply/{baseProjectId}")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> applyToBase(
            @PathVariable Long baseProjectId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = companyService.applyToBaseProject(baseProjectId);
        var rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Draft application created");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Send Project to Review (Company)")
    @PostMapping("/{id}/send-to-review")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> sendToReview(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = companyService.sendToReview(id);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Import CSV (Company register for existing projects)")
    @PostMapping(path = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TuongCommonResponse<ImportReport>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ImportReport result = companyService.importCsv(file);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, result));
    }
}

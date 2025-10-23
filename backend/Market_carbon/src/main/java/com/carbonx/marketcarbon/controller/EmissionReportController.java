package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.CvaVerificationRequest;
import com.carbonx.marketcarbon.dto.response.EmissionReportDetailResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.service.EmissionReportService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class EmissionReportController {

    private final EmissionReportService service;

    private static String traceOrNew(String traceHeader) {
        return (traceHeader != null && !traceHeader.isBlank()) ? traceHeader : UUID.randomUUID().toString();
    }

    private static String dateOrNow(String dateHeader) {
        return (dateHeader != null && !dateHeader.isBlank())
                ? dateHeader
                : OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    @Operation(
            summary = "Upload emission report CSV (Company only)",
            description = "Allows a company to upload a CSV file to generate an emission report. Accepts projectId as a form field; CSV may also contain project_id column. If both present, they must match."
    )
    @PreAuthorize("hasRole('COMPANY')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TuongCommonResponse<EmissionReportResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.uploadCsvAsReport(file, projectId);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Report uploaded successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }


    @Operation(
            summary = "List emission reports for CVA review",
            description = "Retrieves a list of submitted emission reports for CVA to review and verify. Can be filtered by status."
    )
    @PreAuthorize("hasRole('CVA')")
    @GetMapping("/list-cva-check")
    public ResponseEntity<TuongCommonResponse<List<EmissionReportResponse>>> listForCva(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now = dateOrNow(requestDateTime);

        Page<EmissionReportResponse> p = service.listReportsForCva(status, PageRequest.of(page, size));
        List<EmissionReportResponse> data = p.getContent();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Reports fetched successfully for CVA");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @Operation(
            summary = "Download original uploaded CSV file",
            description = "Allows CVA or Admin to download the original CSV file uploaded by the company."
    )
    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    @GetMapping(value = "/{reportId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable Long reportId) {
        byte[] bytes = service.downloadCsv(reportId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=original_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(
            summary = "Export summarized emission report (CSV)",
            description = "Exports a summarized CSV containing project, period, total energy, total CO2, etc. for a given report."
    )
    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    @GetMapping(value = "/{reportId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Long reportId) {
        byte[] bytes = service.exportSummaryCsv(reportId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=emission_report_" + reportId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @Operation(
            summary = "Verify emission report (CVA)",
            description = "Allows CVA to verify the accuracy of a report. Can approve or reject the report with an optional comment."
    )
    @PreAuthorize("hasRole('CVA')")
    @PutMapping("/{reportId}/verify")
    public ResponseEntity<TuongCommonResponse<EmissionReportResponse>> verify(
            @PathVariable Long reportId,
            @RequestParam("approved") boolean approved,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.verifyReport(reportId, approved, comment);

        String msg = approved ? "Report verified successfully" : "Report rejected by CVA";
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), msg);
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @Operation(
            summary = "Final approval by Admin",
            description = "Allows Admin to perform the final approval step after CVA verification."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reportId}/approve")
    public ResponseEntity<TuongCommonResponse<EmissionReportResponse>> approve(
            @PathVariable Long reportId,
            @RequestParam("approved") boolean approved,
            @RequestParam(value = "note", required = false) String note,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.adminApproveReport(reportId, approved, note);

        String msg = approved ? "Report approved by Admin" : "Report rejected by Admin";
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), msg);
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @Operation(
            summary = "List all emission reports (Admin only)",
            description = "Retrieves a paginated list of all emission reports across all companies. Can filter by status."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<TuongCommonResponse<List<EmissionReportResponse>>> listReportsForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now = dateOrNow(requestDateTime);

        Page<EmissionReportResponse> p = service.listReportsForAdmin(status, PageRequest.of(page, size));
        List<EmissionReportResponse> data = p.getContent();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Reports listed successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @Operation(
            summary = "List my emission reports (Company)",
            description = "Allows a company to view all emission reports it has submitted, optionally filtered by status."
    )
    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/my-reports")
    public ResponseEntity<TuongCommonResponse<List<EmissionReportResponse>>> listMyReports(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String date
    ) {
        String t = traceOrNew(trace);
        String now = dateOrNow(date);

        List<EmissionReportResponse> data = service.listReportsForCompany(status, projectId);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "My reports fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, t, rs, data));
    }

    @Operation(
            summary = "Get emission report by ID",
            description = "Retrieves a single emission report by ID, including project, period, and summary information."
    )
    @GetMapping("{reportId}")
    public ResponseEntity<CommonResponse<EmissionReportResponse>> getReportById(@PathVariable Long reportId) {
        EmissionReportResponse data = service.getById(reportId);
        return ResponseEntity.ok(ResponseUtil.success(UUID.randomUUID().toString(), data));
    }

    @Operation(
            summary = "Get per-vehicle emission details in a report",
            description = "Retrieves all vehicle-level emission details within a specific report, including total energy and CO2 emissions per vehicle."
    )
    @GetMapping("/{reportId}/details")
    public ResponseEntity<CommonResponse<List<EmissionReportDetailResponse>>> getReportDetails(
            @PathVariable Long reportId) {

        List<EmissionReportDetailResponse> data = service.getReportDetails(reportId);
        return ResponseEntity.ok(ResponseUtil.success(UUID.randomUUID().toString(), data));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('CVA')")
    public EmissionReportResponse verify(
            @PathVariable("id") Long reportId,
            @Valid @RequestBody CvaVerificationRequest req) {
        return service.verifyReportWithScore(reportId, req.verificationScore(), req.approved(), req.comment());
    }
}

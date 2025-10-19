package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.service.EmissionReportService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
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

    @PreAuthorize("hasRole('COMPANY')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TuongCommonResponse<EmissionReportResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = traceOrNew(requestTrace);
        String now   = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.uploadCsvAsReport(file);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<EmissionReportResponse> body = new TuongCommonResponse<>(now, trace, rs, data);
        return ResponseEntity.ok(body);
    }

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
        String now   = dateOrNow(requestDateTime);

        Page<EmissionReportResponse> p = service.listReportsForCva(status, PageRequest.of(page, size));
        List<EmissionReportResponse> data = p.getContent();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<EmissionReportResponse>> body = new TuongCommonResponse<>(now, trace, rs, data);
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    @GetMapping(value = "/{reportId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable Long reportId) {
        byte[] bytes = service.downloadCsv(reportId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=original_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PreAuthorize("hasAnyRole('CVA','ADMIN')")
    @GetMapping(value = "/{reportId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Long reportId) {
        byte[] bytes = service.exportSummaryCsv(reportId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=emission_report_" + reportId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

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
        String now   = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.verifyReport(reportId, approved, comment);

        String msg = approved ? "Report verified" : "Report rejected by CVA";
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), msg);
        TuongCommonResponse<EmissionReportResponse> body = new TuongCommonResponse<>(now, trace, rs, data);
        return ResponseEntity.ok(body);
    }

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
        String now   = dateOrNow(requestDateTime);

        EmissionReportResponse data = service.adminApproveReport(reportId, approved, note);

        String msg = approved ? "Report approved" : "Report rejected by Admin";
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), msg);
        TuongCommonResponse<EmissionReportResponse> body = new TuongCommonResponse<>(now, trace, rs, data);
        return ResponseEntity.ok(body);
    }

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
        String now   = dateOrNow(requestDateTime);

        Page<EmissionReportResponse> p = service.listReportsForAdmin(status, PageRequest.of(page, size));
        List<EmissionReportResponse> data = p.getContent();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "List reports successfully");
        TuongCommonResponse<List<EmissionReportResponse>> body = new TuongCommonResponse<>(now, trace, rs, data);
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/my-reports")
    public ResponseEntity<TuongCommonResponse<List<EmissionReportResponse>>> listMyReports(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String date
    ) {
        String t = traceOrNew(trace);
        String now = dateOrNow(date);

        // Gọi service lấy danh sách report của công ty
        List<EmissionReportResponse> data = service.listReportsForCompany(status);

        TuongResponseStatus rs = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage()
        );

        TuongCommonResponse<List<EmissionReportResponse>> body =
                new TuongCommonResponse<>(now, t, rs, data);

        return ResponseEntity.ok(body);
    }
}






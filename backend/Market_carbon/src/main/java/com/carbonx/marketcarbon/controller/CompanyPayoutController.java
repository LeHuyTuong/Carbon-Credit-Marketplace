package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.response.*;
import com.carbonx.marketcarbon.service.CompanyPayoutQueryService;
import com.carbonx.marketcarbon.service.ReportService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyPayoutController {

    private final CompanyPayoutQueryService companyPayoutQueryService;
    private final ReportService reportExportService;

    @GetMapping("/payout-formula")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Cho công ty biết họ đang dùng công thức nào để trả tiền.",
            description = "xem công ty này trả tiền theo kWh (số điện) hay theo CREDIT (tín chỉ carbon), và đơn giá là bao nhiêu.")
    public ResponseEntity<TuongCommonResponse<PayoutFormulaResponse>> getPayoutFormula(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PayoutFormulaResponse data = companyPayoutQueryService.getPayoutFormula();

        TuongResponseStatus status = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PayoutFormulaResponse> response = new TuongCommonResponse<>(trace, now, status, data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ev-owners")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "List EV owners contributing to a company during a period")
    public ResponseEntity<TuongCommonResponse<PageResponse<List<CompanyEVOwnerSummaryResponse>>>> listCompanyOwners(
            @RequestParam String period,
            @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PageResponse<List<CompanyEVOwnerSummaryResponse>> data = companyPayoutQueryService
                .listCompanyOwners( period, page, size, search);

        TuongResponseStatus status = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PageResponse<List<CompanyEVOwnerSummaryResponse>>> response =
                new TuongCommonResponse<>(trace, now, status, data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/{reportId}/owners")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Preview EV owner payouts for an emission report",
              description = "Tính toán tổng số tiền công ty phải trả cho từng EV owner theo chính sách payout (USD).")
    public ResponseEntity<TuongCommonResponse<CompanyReportOwnersResponse>> listCompanyOwnersForReport(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ownerName,asc") String sort,
            @RequestParam(defaultValue = "2") int scale,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        CompanyReportOwnersResponse data = companyPayoutQueryService.listCompanyOwnersForReport(
                reportId,
                page,
                size,
                sort,
                scale);
        TuongResponseStatus status = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<CompanyReportOwnersResponse> response =
                new TuongCommonResponse<>(trace, now, status, data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payouts/{distributionId}/summary")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Get payout summary for a distribution")
    public ResponseEntity<TuongCommonResponse<CompanyPayoutSummaryResponse>> getPayoutSummary(
            @PathVariable Long distributionId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CompanyPayoutSummaryResponse data = companyPayoutQueryService.getDistributionSummary( distributionId);

        TuongResponseStatus status = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<CompanyPayoutSummaryResponse> response = new TuongCommonResponse<>(trace, now, status, data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payouts/{distributionId}/export.xlsx")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Export payout summary to Excel")
    public ResponseEntity<byte[]> exportPayoutSummary(
            @PathVariable Long distributionId) {

        byte[] content = reportExportService.exportCompanyPayoutXlsx(distributionId);
        String filename = String.format("payout_%d.xlsx", distributionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }
}

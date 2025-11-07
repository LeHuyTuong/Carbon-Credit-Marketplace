package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyCreditStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyReportStatusDto;
import com.carbonx.marketcarbon.dto.dashboard.SummaryValue;
import com.carbonx.marketcarbon.service.DashboardCardService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cva/dashboard")
@RequiredArgsConstructor
public class CvaDashboardController {

    private final DashboardCardService service;

    // Tạo trace ID nếu không có
    private static String traceOrNew(String traceHeader) {
        return (traceHeader != null && !traceHeader.isBlank())
                ? traceHeader
                : UUID.randomUUID().toString();
    }

    // Lấy thời gian hiện tại nếu không có trong header
    private static String dateOrNow(String dateHeader) {
        return (dateHeader != null && !dateHeader.isBlank())
                ? dateHeader
                : OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    // Tổng hợp cả 4 card
    @Operation(summary = "Get summary dashboard cards for CVA (Reports, Credits, Companies, Projects)")
    @GetMapping("/cards")
    public ResponseEntity<TuongCommonResponse<Map<String, SummaryValue>>> getAllCards(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        Map<String, SummaryValue> data = new HashMap<>();
        data.put("reports", service.getReportSummary());
        data.put("credits", service.getCreditSummary());
        data.put("companies", service.getCompanySummary());
        data.put("projects", service.getProjectSummary());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Dashboard data fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    //  Reports
    @Operation(summary = "Get report card summary")
    @GetMapping("/reports")
    public ResponseEntity<TuongCommonResponse<SummaryValue>> getReportsCard(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        SummaryValue data = service.getReportSummary();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Report summary fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    //  Credits
    @Operation(summary = "Get credit card summary")
    @GetMapping("/credits")
    public ResponseEntity<TuongCommonResponse<SummaryValue>> getCreditsCard(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        SummaryValue data = service.getCreditSummary();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Credit summary fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    //  Companies
    @Operation(summary = "Get company card summary")
    @GetMapping("/companies")
    public ResponseEntity<TuongCommonResponse<SummaryValue>> getCompaniesCard(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        SummaryValue data = service.getCompanySummary();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Company summary fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    //  Projects
    @Operation(summary = "Get project card summary")
    @GetMapping("/projects")
    public ResponseEntity<TuongCommonResponse<SummaryValue>> getProjectsCard(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        SummaryValue data = service.getProjectSummary();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Project summary fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @Operation(summary = "Get monthly report status statistics (approved/pending/rejected)")
    @GetMapping("/reports/status")
    public ResponseEntity<TuongCommonResponse<List<MonthlyReportStatusDto>>> getReportStatusChart(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        List<MonthlyReportStatusDto> data = service.getMonthlyReportStatus();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Report status chart fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }

    @GetMapping("/credits/status/monthly")
    public ResponseEntity<TuongCommonResponse<List<MonthlyCreditStatusDto>>> getMonthlyCreditStatus(
            @RequestHeader(value = "X-Request-Trace", required = false) String traceHeader,
            @RequestHeader(value = "X-Request-DateTime", required = false) String dateHeader
    ) {
        String trace = traceOrNew(traceHeader);
        String now = dateOrNow(dateHeader);

        List<MonthlyCreditStatusDto> data = service.getMonthlyCreditStatus();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Monthly credit status fetched successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(now, trace, rs, data));
    }


}

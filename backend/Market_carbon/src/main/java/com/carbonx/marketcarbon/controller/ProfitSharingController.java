package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.service.ProfitSharingService;
import com.carbonx.marketcarbon.service.UserService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profit-sharing")
@RequiredArgsConstructor
public class ProfitSharingController {


    private final ProfitSharingService profitSharingService;

    @PostMapping("/share")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Trigger company payout to EV owners",
            description = "Thực thi thanh toán (payout) cho EV owners dựa trên chính sách cố định (ví dụ: USD/kWh hoặc USD/credit) cho một report cụ thể.")
    public  ResponseEntity<TuongCommonResponse<Void>> shareProfit(
            @Valid @RequestBody TuongCommonRequest<ProfitSharingRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        // Lấy payload data
        ProfitSharingRequest payload = request.getData();

        // BẮT BUỘC PHẢI CÓ EMISSION REPORT ID
        if (payload == null || payload.getEmissionReportId() == null) {
            throw new BadRequestException("Emission report id is required");
        }

        // Gọi phương thức async
        profitSharingService.shareCompanyProfit(payload);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                "Payout process started successfully. This will run in the background.");
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);

    }
}

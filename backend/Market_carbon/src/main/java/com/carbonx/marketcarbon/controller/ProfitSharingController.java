package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.service.ProfitSharingService;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
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
    private final UserService userService;

    /**
     * Kích hoạt quá trình chia sẻ lợi nhuận.
     * Chỉ COMPANY mới có thể gọi.
     *
     * @param request DTO chứa tổng tiền, mô tả, và (tùy chọn) ID của report.
     * @return 202 ACCEPTED - Yêu cầu đã được tiếp nhận và đang xử lý ngầm.
     */
    @PostMapping("/share")
    @PreAuthorize("hasRole('COMPANY')")
    public  ResponseEntity<TuongCommonResponse<Void>> shareProfit(
            @Valid @RequestBody TuongCommonRequest<ProfitSharingRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        // Gọi phương thức async
        profitSharingService.shareCompanyProfit(request.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);

    }
}

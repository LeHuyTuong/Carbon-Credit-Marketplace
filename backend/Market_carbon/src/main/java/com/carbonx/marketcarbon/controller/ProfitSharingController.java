package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.service.ProfitSharingService;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.utils.common.ApiRequest;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/share")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Trigger company payout to EV owners",
            description = "Thực thi thanh toán (payout) cho EV owners dựa trên chính sách cố định (ví dụ: USD/kWh hoặc USD/credit) cho một report cụ thể.")
    public  ResponseEntity<ApiResponse<Void>> shareProfit(
            @Valid @RequestBody ApiRequest<ProfitSharingRequest> request,
            Authentication authentication,
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

        // Xác thực caller ở request thread rồi truyền ID sang async thread.
        // Async thread không có SecurityContext nên không được đọc lại ở service.
        User companyUser = userService.findUserByEmail(authentication.getName());
        if (companyUser == null) {
            throw new BadRequestException("Authenticated user not found");
        }

        // Gọi phương thức async
        profitSharingService.shareCompanyProfit(companyUser.getId(), payload);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                "Payout process started successfully. This will run in the background.");
        ApiResponse<Void> response = new ApiResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);

    }
}

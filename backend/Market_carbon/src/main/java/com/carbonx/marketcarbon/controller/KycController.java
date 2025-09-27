package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.ApiResponse;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import com.carbonx.marketcarbon.utils.CommonRequest;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    public ResponseEntity<CommonResponse<KycResponse>> create(
            @Valid @RequestBody CommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse created = kycService.create(req.getRequestParamters());

        return ResponseEntity.ok(ResponseUtil.success(trace, created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<KycResponse>> update(
            @PathVariable Long id,
            @RequestBody CommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace
            ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse updated = kycService.update(id, req.getRequestParamters());
        return ResponseEntity.ok(ResponseUtil.success(trace, updated));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<KycResponse>> getByUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Request-Trace",required = false) String requestTrace){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse kyc =  kycService.getByUser(userId);
        return ResponseEntity.ok(ResponseUtil.success(trace, kyc));
    }


}

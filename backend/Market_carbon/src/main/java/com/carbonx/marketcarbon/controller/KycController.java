package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.ApiResponse;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    public ResponseEntity<CommonResponse<KycResponse>> create(
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse created = kycService.create(req.getData());

        return ResponseEntity.ok(ResponseUtil.success(trace, created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<KycResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<KycRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace
            ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse updated = kycService.update(id, req.getData());
        return ResponseEntity.ok(ResponseUtil.success(trace, updated));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<KycResponse>> getByUser(
            @PathVariable(name="userId") @Valid Long userId,
            @RequestHeader(value = "X-Request-Trace",required = false) String requestTrace){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        KycResponse kyc =  kycService.getByUserId(userId);
        return ResponseEntity.ok(ResponseUtil.success(trace, kyc));
    }


}

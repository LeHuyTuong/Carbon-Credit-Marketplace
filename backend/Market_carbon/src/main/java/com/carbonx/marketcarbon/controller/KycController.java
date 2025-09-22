package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.ApiResponse;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService service;

    @PostMapping
    public ResponseEntity<ApiResponse<KycResponse>> create(@Valid @RequestBody KycRequest req){
        return ResponseEntity.ok(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KycResponse>> update(@PathVariable Long id, @RequestBody KycRequest req){
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<KycResponse>> getByUser(@PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.ok(service.getByUser(userId)));
    }
}

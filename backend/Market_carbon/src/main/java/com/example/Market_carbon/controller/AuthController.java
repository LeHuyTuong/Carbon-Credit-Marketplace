package com.example.Market_carbon.controller;

import com.example.Market_carbon.request.LoginRequest;
import com.example.Market_carbon.request.RegisterRequest;
import com.example.Market_carbon.request.VerifyOtpRequest;
import com.example.Market_carbon.response.ApiResponse;
import com.example.Market_carbon.response.MessageResponse;
import com.example.Market_carbon.response.TokenResponse;
import com.example.Market_carbon.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MessageResponse>> register(@Valid @RequestBody RegisterRequest req){
        return ResponseEntity.ok(ApiResponse.ok(authService.register(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MessageResponse>> login(@Valid @RequestBody LoginRequest req){
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req){
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@RequestHeader(name="Authorization", required = false) String bearer){
        return ResponseEntity.ok(ApiResponse.ok(authService.logout(bearer)));
    }
}

package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.service.AuthService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-register", authService.register(req)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-verify-otp", authService.verifyOtp(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-login", authService.login(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<MessageResponse>> logout(
            @RequestHeader(name = "Authorization", required = false) String bearer) {
        return ResponseEntity.ok(ResponseUtil.success("trace-logout", authService.logout(bearer)));
    }

    @PostMapping("/reset-token")
    public ResponseEntity<CommonResponse<MessageResponse>> resetPassword(@RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-reset-password", authService.resetPassword(req)));
    }

    @PostMapping("/reset-password-request")
    public ResponseEntity<CommonResponse<MessageResponse>> resetPasswordRequest(@RequestParam("email") String email) {
        return ResponseEntity.ok(ResponseUtil.success("trace-reset-password-request", authService.resetPasswordRequest(email)));
    }
}

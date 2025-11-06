package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.config.OAuth2UserWithToken;
import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.service.AuthService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-register", authService.register(req)));
    }

    @Operation(summary = "Verify OTP to activate account")
    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-verify-otp", authService.verifyOtp(req)));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ResponseUtil.success("trace-login", authService.login(req)));
    }

    @Operation(summary = "Logout and invalidate current token")
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<MessageResponse>> logout(
            @RequestHeader(name = "Authorization", required = false) String bearer) {
        return ResponseEntity.ok(ResponseUtil.success("trace-logout", authService.logout(bearer)));
    }

    @Operation(summary = "Handle successful Google OAuth2 login")
    @GetMapping("/oauth2/success")
    public Map<String, Object> oauth2Success(@AuthenticationPrincipal OAuth2UserWithToken user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login success");
        response.put("token", user.getToken());
        response.put("email", user.getAttributes().get("email"));
        response.put("role", "EV_OWNER");
        return response;
    }

}

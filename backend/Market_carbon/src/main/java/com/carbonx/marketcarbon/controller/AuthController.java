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

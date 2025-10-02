package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.TokenResponse;


public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse verifyOtp(VerifyOtpRequest req);
    AuthResponse login(LoginRequest req);
    MessageResponse logout(String bearerToken);
    MessageResponse resetPassword(ResetPasswordRequest req);
    MessageResponse resetPasswordRequest(String email);
}

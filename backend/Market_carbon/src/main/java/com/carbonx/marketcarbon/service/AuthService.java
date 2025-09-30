package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.TokenResponse;


public interface AuthService {
    MessageResponse register(RegisterRequest request);
    MessageResponse login(LoginRequest request);
    TokenResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse logout(String bearerToken);
}

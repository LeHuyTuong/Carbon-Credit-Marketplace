package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.request.LoginRequest;
import com.carbonx.marketcarbon.request.RegisterRequest;
import com.carbonx.marketcarbon.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.response.MessageResponse;
import com.carbonx.marketcarbon.response.TokenResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);
    MessageResponse login(LoginRequest request);
    TokenResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse logout(String bearerToken);
}

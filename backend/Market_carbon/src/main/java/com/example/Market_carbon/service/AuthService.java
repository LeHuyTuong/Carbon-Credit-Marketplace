package com.example.Market_carbon.service;

import com.example.Market_carbon.request.LoginRequest;
import com.example.Market_carbon.request.RegisterRequest;
import com.example.Market_carbon.request.VerifyOtpRequest;
import com.example.Market_carbon.response.MessageResponse;
import com.example.Market_carbon.response.TokenResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);
    MessageResponse login(LoginRequest request);
    TokenResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse logout(String bearerToken);
}

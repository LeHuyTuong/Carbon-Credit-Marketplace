package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.TokenResponse;
import com.carbonx.marketcarbon.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Override
    public MessageResponse register(RegisterRequest request) {
        return null;
    }

    @Override
    public MessageResponse login(LoginRequest request) {
        return null;
    }

    @Override
    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        return null;
    }

    @Override
    public MessageResponse logout(String bearerToken) {
        return null;
    }
}

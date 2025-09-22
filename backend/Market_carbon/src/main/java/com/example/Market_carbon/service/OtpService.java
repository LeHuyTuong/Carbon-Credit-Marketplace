package com.example.Market_carbon.service;

public interface OtpService {
    void sendOtpToEmail(String email);
    boolean verifyOtp(String email, String code);
}

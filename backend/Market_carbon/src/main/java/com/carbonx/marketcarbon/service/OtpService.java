package com.carbonx.marketcarbon.service;

public interface OtpService {
    void sendOtpToEmail(String email);
    boolean verifyOtp(String email, String code);
}

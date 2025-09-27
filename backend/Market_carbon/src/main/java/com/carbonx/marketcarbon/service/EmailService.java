package com.carbonx.marketcarbon.service;

public interface EmailService {
    void sendEmail(String email, String otp);
    String generateOtp();
    String encodeOtp(String otp);
}

package com.carbonx.marketcarbon.service.impl;

import java.time.Duration;
import java.util.Optional;


public interface OtpService {
    void saveOtp(String email, String otp);                            // TTL mặc định 10'
    void saveOtp(String email, String otp, Duration ttl);              // TTL tuỳ biến
    Optional<String> getOtp(String email);
    void deleteOtp(String email);
}
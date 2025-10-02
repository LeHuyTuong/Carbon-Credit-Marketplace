package com.carbonx.marketcarbon.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpServiceImpl implements OtpService {

    StringRedisTemplate redis;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Override
    public void saveOtp(String email, String otp) {
        saveOtp(email, otp, DEFAULT_TTL);
    }

    @Override
    public void saveOtp(String email, String otp, Duration ttl) {
        String k = key(email);
        redis.opsForValue().set(k, otp, ttl);
    }

    @Override
    public Optional<String> getOtp(String email) {
        String v = redis.opsForValue().get(key(email));
        return Optional.ofNullable(v);
    }

    @Override
    public void deleteOtp(String email) {
        redis.delete(key(email));
    }

    private String key(String email) {
        return "otp:forgot:" + (email == null ? "" : email.trim().toLowerCase());
    }
}
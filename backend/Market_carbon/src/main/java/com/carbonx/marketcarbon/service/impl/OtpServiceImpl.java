package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.service.OtpService;

public class OtpServiceImpl implements OtpService {
    @Override
    public void sendOtpToEmail(String email) {

    }

    @Override
    public boolean verifyOtp(String email, String code) {
        return false;
    }
}

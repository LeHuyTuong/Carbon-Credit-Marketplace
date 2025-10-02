package com.carbonx.marketcarbon.service;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface EmailService {

    public void sendEmail(String subject, String content, List<String> toList) throws MessagingException,
            UnsupportedEncodingException;
}

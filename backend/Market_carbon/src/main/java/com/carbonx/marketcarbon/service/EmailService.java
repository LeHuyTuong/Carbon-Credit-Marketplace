package com.carbonx.marketcarbon.service;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public interface EmailService {

    public void sendEmail(String subject, String content, List<String> toList) throws MessagingException,
            UnsupportedEncodingException;
    String renderCvaDecisionEmail(Map<String, Object> variables);
    String renderAdminDecisionEmail(Map<String, Object> variables);

    void send(String to, String subject, String body) throws MessagingException;

    void sendHtml(String to, String subject, String html) throws MessagingException;
    void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] file, String filename);


    String renderWithdrawalConfirmationEmail(Map<String, Object> variables);
}



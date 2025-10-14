package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    @NonFinal
    @Value("${spring.mail.username}")
    String emailFrom;

    JavaMailSender mailSender;
    SpringTemplateEngine templateEngine;

    @Async
    public void sendEmail(String subject, String content, List<String> toList) throws MessagingException,
            UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom(emailFrom, "CarbonX team");
        helper.setTo(toList.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(content, true);

        try {
            mailSender.send(mimeMessage);
            log.info("Email sent to {}", toList);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toList, e.getMessage(), e);
        }
    }
}

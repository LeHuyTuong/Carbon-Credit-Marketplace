package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;
    SpringTemplateEngine templateEngine;

    @NonFinal
    @Value("${spring.mail.username}")
    String emailFrom;

    // Cho phép đổi tên hiển thị qua cấu hình; mặc định "CarbonX team"
    @NonFinal
    @Value("${app.mail.display-name:CarbonX team}")
    String displayName;

    @Async
    public void sendEmail(String subject, String content, List<String> toList) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            try {
                helper.setFrom(emailFrom, displayName);
            } catch (UnsupportedEncodingException e) {
                log.warn("Unsupported sender display name encoding. Fallback to bare address.");
                helper.setFrom(emailFrom);
            }
            helper.setTo(toList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(content, true); // HTML

            mailSender.send(mimeMessage);
            log.info("Email sent to {}", toList);
        } catch (MessagingException ex) {
            log.error("Failed to build or send email to {}: {}", toList, ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to {}: {}", toList, ex.getMessage(), ex);
        }
    }

    @Override
    public String renderCvaDecisionEmail(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process("emails/cva-decision.html", ctx);
    }

    @Override
    public String renderAdminDecisionEmail(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process("emails/admin-decision.html", ctx);
    }

    @Async
    @Override
    public void send(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        try {
            try {
                helper.setFrom(emailFrom, displayName);
            } catch (UnsupportedEncodingException e) {
                log.warn("Unsupported sender display name encoding. Fallback to bare address.");
                helper.setFrom(emailFrom);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // plain text
            mailSender.send(message);
            log.info("Plain text email sent to {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send plain text email to {}: {}", to, ex.getMessage(), ex);
            throw ex; // giữ hành vi theo interface nếu cần bắt ở tầng gọi
        }
    }

    @Async
    @Override
    public void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        try {
            try {
                helper.setFrom(emailFrom, displayName);
            } catch (UnsupportedEncodingException e) {
                log.warn("Unsupported sender display name encoding. Fallback to bare address.");
                helper.setFrom(emailFrom);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // HTML
            mailSender.send(message);
            log.info("HTML email sent to {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send HTML email to {}: {}", to, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] file, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress("hoang106408@donga.edu.vn", "CarbonX Marketplace"));
            helper.setTo(to);
            helper.setSubject(subject);

            helper.setText(htmlBody, true);

            if (file != null && file.length > 0 && filename != null) {
                helper.addAttachment(filename, new ByteArrayResource(file));
            }

            mailSender.send(message);
            log.info("[MAIL] Sent certificate email to {}", to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[MAIL] Failed to send email with attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }


    public String renderWithdrawalConfirmationEmail(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process("emails/withdrawal-confirmation.html", ctx);
    }
}

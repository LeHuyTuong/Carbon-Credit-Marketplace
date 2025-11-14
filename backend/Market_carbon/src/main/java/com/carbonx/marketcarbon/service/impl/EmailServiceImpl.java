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
import java.math.BigDecimal;
import java.util.HashMap; // Đảm bảo import
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    @NonFinal
    @Value("${spring.mail.enabled:true}")
    boolean mailEnabled;


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
            throw ex;
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

    @Override
    public String renderWithdrawalFailedEmail(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process("emails/withdrawal-failed.html", ctx);
    }

    @Override
    public String renderReportCvaDecisionEmail(Map<String, Object> vars) {
        return templateEngine.process("emails/report-cva-decision.html", new Context(Locale.getDefault(), vars));
    }

    @Override
    public String renderReportAdminDecisionEmail(Map<String, Object> vars) {
        return templateEngine.process("emails/report-admin-decision.html", new Context(Locale.getDefault(), vars));
    }

    // --- CÁC HÀM MỚI ĐÂY ---
    @Override
    public String renderPayoutSuccessEmail(Map<String, Object> variables) {
        Context ctx = new Context(Locale.getDefault(), variables);
        return templateEngine.process("emails/payout-success.html", ctx);
    }

    @Override
    public String renderPayoutSummaryEmail(Map<String, Object> variables) {
        Context ctx = new Context(Locale.getDefault(), variables);
        return templateEngine.process("emails/payout-summary.html", ctx);
    }

    @Async("profitSharingTaskExecutor")
    @Override
    public void sendPayoutSuccessToOwner(String toEmail,
                                         String ownerName,
                                         String companyName,
                                         String periodLabel,
                                         BigDecimal totalEnergyKWh,
                                         BigDecimal totalCredits,
                                         BigDecimal amountUsd,
                                         List<VehiclePayoutRow> perVehicle,
                                         String distributionReference,
                                         Long companyId,
                                         String reportReference,
                                         BigDecimal minPayout) {
        if (!mailEnabled) {
            log.info("Mail disabled. Skipping payout success email to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Missing recipient email for payout success notification. Owner: {}", ownerName);
            return;
        }

        // 1. Tạo Context Map cho Thymeleaf
        Map<String, Object> vars = new HashMap<>();
        vars.put("ownerName", ownerName);
        vars.put("companyName", companyName);
        vars.put("periodLabel", periodLabel);
        vars.put("totalEnergyKWh", totalEnergyKWh);
        vars.put("totalCredits", totalCredits);
        vars.put("amountUsd", amountUsd);
        vars.put("perVehicle", perVehicle);
        vars.put("distributionReference", distributionReference);
        vars.put("companyId", companyId);
        vars.put("reportReference", reportReference);
        vars.put("minPayout", minPayout);

        try {
            // 2. Render HTML
            String subject = String.format("[CarbonX] Bạn nhận được thanh toán kỳ %s từ %s", periodLabel, companyName);
            String htmlBody = renderPayoutSuccessEmail(vars);

            // 3. Gửi bằng sendHtml
            sendHtml(toEmail, subject, htmlBody);

        } catch (Exception ex) {
            log.warn("Failed to send HTML payout notification email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Async("profitSharingTaskExecutor")
    @Override
    public void sendDistributionSummaryToCompany(String toEmail,
                                                 String companyName,
                                                 String periodLabel,
                                                 int ownersPaid,
                                                 BigDecimal totalEnergy,
                                                 BigDecimal totalCredits,
                                                 BigDecimal totalPayoutUsd,
                                                 boolean scaledByCap,
                                                 Long companyId,
                                                 String distributionReference) {
        if (!mailEnabled) {
            log.info("Mail disabled. Skipping payout summary email to company {}", companyName);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Missing recipient email for payout summary notification. Company: {}", companyName);
            return;
        }

        // 1. Tạo Context Map
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", companyName);
        vars.put("periodLabel", periodLabel);
        vars.put("ownersPaid", ownersPaid);
        vars.put("totalEnergy", totalEnergy);
        vars.put("totalCredits", totalCredits);
        vars.put("totalPayoutUsd", totalPayoutUsd);
        vars.put("scaledByCap", scaledByCap);
        vars.put("companyId", companyId);
        vars.put("distributionReference", distributionReference);

        try {
            // 2. Render HTML
            String subject = String.format("[CarbonX] Tổng kết payout kỳ %s", periodLabel);
            String htmlBody = renderPayoutSummaryEmail(vars);

            // 3. Gửi bằng sendHtml
            sendHtml(toEmail, subject, htmlBody);

        } catch (Exception ex) {
            log.warn("Failed to send HTML payout summary email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }
}

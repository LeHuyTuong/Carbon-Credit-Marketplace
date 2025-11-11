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
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
    boolean mailEnabled;   // <— now excluded from required-args constructor


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

    @Override
    public String renderWithdrawalFailedEmail(Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        // Sử dụng template mới emails/withdrawal-failed.html
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

    @Async("profitSharingTaskExecutor")
    @Override
    public void sendPayoutSuccessToOwner(String toEmail,
                                         String ownerName,
                                         String companyName,
                                         String periodLabel,
                                         BigDecimal totalEnergyKWh,
                                         BigDecimal totalCredits,
                                         BigDecimal amountVND,
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

        String subject = String.format("[CarbonX] Payout tháng %s từ %s", periodLabel, companyName);
        String body = buildOwnerBody(ownerName, companyName, periodLabel, totalEnergyKWh, totalCredits, amountVND,
                perVehicle, distributionReference, companyId, reportReference, minPayout);

        sendPlainTextEmail(toEmail, subject, body);
    }

    @Async("profitSharingTaskExecutor")
    @Override
    public void sendDistributionSummaryToCompany(String toEmail,
                                                 String companyName,
                                                 String periodLabel,
                                                 int ownersPaid,
                                                 BigDecimal totalEnergy,
                                                 BigDecimal totalCredits,
                                                 BigDecimal totalPayoutVND,
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

        String subject = String.format("[CarbonX] Tổng kết payout kỳ %s", periodLabel);
        StringBuilder body = new StringBuilder();
        body.append("Xin chào ").append(companyName).append("\n\n")
                .append("Tổng số EV Owner đã được thanh toán: ").append(ownersPaid).append("\n")
                .append("Tổng năng lượng: ").append(formatDecimal(totalEnergy)).append(" kWh\n")
                .append("Tổng tín chỉ: ").append(formatDecimal(totalCredits)).append(" tCO₂e\n")
                .append("Tổng số tiền đã chi: ").append(formatCurrency(totalPayoutVND)).append(" VND\n\n");

        if (scaledByCap) {
            body.append("Lưu ý: Số tiền payout đã được điều chỉnh theo tổng ngân sách được đặt trong kỳ này.\n\n");
        }

        body.append("Xem chi tiết đợt payout: /companies/").append(companyId)
                .append("/payouts/").append(distributionReference).append("\n");
        body.append("Link export Excel: /companies/").append(companyId)
                .append("/payouts/").append(distributionReference).append("/export.xlsx\n");

        sendPlainTextEmail(toEmail, subject, body.toString());
    }

    private void sendPlainTextEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            if (!emailFrom.isBlank()) {
                message.setFrom(String.format("%s <%s>", displayName, emailFrom).trim());
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent notification email to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Failed to send notification email to {}: {}", toEmail, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error when sending notification email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private String buildOwnerBody(String ownerName,
                                  String companyName,
                                  String periodLabel,
                                  BigDecimal totalEnergyKWh,
                                  BigDecimal totalCredits,
                                  BigDecimal amountVND,
                                  List<VehiclePayoutRow> perVehicle,
                                  String distributionReference,
                                  Long companyId,
                                  String reportReference,
                                  BigDecimal minPayout) {
        StringBuilder body = new StringBuilder();
        body.append("Xin chào ").append(ownerName).append("\n\n")
                .append("Tổng năng lượng bạn đã đóng góp trong kỳ ").append(periodLabel).append(": ")
                .append(formatDecimal(totalEnergyKWh)).append(" kWh\n")
                .append("Tổng tín chỉ (credits): ").append(formatDecimal(totalCredits)).append(" tCO₂e\n")
                .append("Số tiền nhận trong kỳ: ").append(formatCurrency(amountVND)).append(" VND\n\n");

        if (perVehicle != null && !perVehicle.isEmpty()) {
            body.append("Biển số | Tên xe | kWh | Credits | Số tiền (VND)\n");
            String rows = perVehicle.stream()
                    .map(row -> String.join(" | ",
                            safe(row.plate()),
                            safe(row.vehicleNameOrModel()),
                            formatDecimal(row.energyKWh()),
                            formatDecimal(row.credits()),
                            formatCurrency(row.amountVND())))
                    .collect(Collectors.joining("\n"));
            body.append(rows).append("\n\n");
        }

        body.append("Thông tin report: ").append(reportReference)
                .append(" do ").append(companyName).append(" nộp\n");
        body.append("Link xem chi tiết: /companies/").append(companyId)
                .append("/payouts/").append(distributionReference).append("\n");

        if (minPayout != null) {
            body.append("Lưu ý: Các khoản payout dưới ")
                    .append(formatCurrency(minPayout))
                    .append(" VND sẽ được cộng dồn sang kỳ sau theo chính sách hiện hành.\n");
        }

        return body.toString();
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return formatNumber(value, 0);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return formatNumber(value, 2);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatNumber(BigDecimal value, int fractionDigits) {
        Locale locale = new Locale("vi", "VN");
        var format = java.text.NumberFormat.getInstance(locale);
        format.setMaximumFractionDigits(fractionDigits);
        format.setMinimumFractionDigits(fractionDigits);
        format.setGroupingUsed(true);
        return format.format(value);
    }
}

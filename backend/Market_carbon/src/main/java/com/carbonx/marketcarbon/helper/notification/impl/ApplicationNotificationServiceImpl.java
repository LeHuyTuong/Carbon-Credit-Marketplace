package com.carbonx.marketcarbon.helper.notification.impl;

import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationServiceImpl implements ApplicationNotificationService {

    private final EmailService emailService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    @Async
    @Override
    public void sendCvaDecision(
            String email,
            String companyName,
            Long applicationId,
            String projectName,
            String reviewerName,
            boolean approved,
            String note
    ) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", companyName);
        vars.put("applicationId", applicationId);
        vars.put("projectName", projectName);
        vars.put("reviewerName", reviewerName);
        vars.put("reviewNote", note);
        vars.put("decision", approved ? "Approved by CVA" : "Rejected by CVA");
        vars.put("badgeColor", approved ? "#16a34a" : "#dc2626");

        String subject = approved
                ? "[CarbonX] Your application was approved by CVA"
                : "[CarbonX] Your application was rejected by CVA";

        try {
            String html = emailService.renderCvaDecisionEmail(vars);
            emailService.sendHtml(email, subject, html);
            log.info(" CVA decision email sent to {}", email);
        } catch (Exception e) {
            log.warn(" Failed to send CVA decision email to {}: {}", email, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendAdminDecision(
            String email,
            String companyName,
            Long applicationId,
            String projectName,
            String reviewerName,
            boolean approved,
            String note
    ) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", companyName);
        vars.put("applicationId", applicationId);
        vars.put("projectName", projectName);
        vars.put("reviewerName", reviewerName);
        vars.put("reviewNote", note);
        vars.put("decision", approved ? "Final Approval" : "Final Rejection");
        vars.put("badgeColor", approved ? "#16a34a" : "#dc2626");

        String subject = approved
                ? "[CarbonX] Final approval granted for your application"
                : "[CarbonX] Final rejection of your application";

        try {
            String html = emailService.renderAdminDecisionEmail(vars);
            emailService.sendHtml(email, subject, html);
            log.info(" Admin decision email sent to {}", email);
        } catch (Exception e) {
            log.warn(" Failed to send Admin decision email to {}: {}", email, e.getMessage());
        }
    }

    @Async // Đảm bảo chạy bất đồng bộ
    @Override
    public void sendAdminConfirmRequestWithdrawal(
            String userEmail,
            String userName,
            Long withdrawalId,
            BigDecimal amount,
            LocalDateTime requestedAt
    ) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", userName);
        vars.put("withdrawalId", withdrawalId);
        vars.put("amount", amount.toPlainString()); // Chuyển BigDecimal thành String để hiển thị
        // Định dạng lại thời gian cho dễ đọc trong email
        vars.put("processedAt", requestedAt.format(DATETIME_FORMATTER));

        String subject = "[CarbonX] Your request withdrawal is approved";

        try {
            String html = emailService.renderWithdrawalConfirmationEmail(vars);
            emailService.sendHtml(userEmail, subject, html);
            log.info(" Withdrawal confirmation email sent to {}", userEmail);
        } catch (Exception e) {
            log.warn(" Failed to send withdrawal confirmation email to {}: {}", userEmail, e.getMessage());
        }
    }

    @Override
    public void sendWithdrawalFailedOrRejected(
            String userEmail,
            String userName,
            Long withdrawalId,
            BigDecimal amount,
            String reason,
            LocalDateTime processedAt) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", userName);
        vars.put("withdrawalId", withdrawalId);
        vars.put("amount", amount.toPlainString());
        vars.put("reason", reason); // Truyền lý do vào template
        vars.put("processedAt", processedAt.format(DATETIME_FORMATTER));
        // Bạn có thể thêm userName nếu cần
        // vars.put("userName", "Tên người dùng"); // Lấy từ User entity nếu cần

        String subject = "[CarbonX] Yêu cầu rút tiền của bạn không thành công";

        try {
            // Render template mới: emails/withdrawal-failed.html
            String html = emailService.renderWithdrawalFailedEmail(vars);
            emailService.sendHtml(userEmail, subject, html);
            log.info(" Withdrawal failed/rejected email sent to {}", userEmail);
        } catch (Exception e) {
            log.warn(" Failed to send withdrawal failed/rejected email to {}: {}", userEmail, e.getMessage());
        }
    }
}

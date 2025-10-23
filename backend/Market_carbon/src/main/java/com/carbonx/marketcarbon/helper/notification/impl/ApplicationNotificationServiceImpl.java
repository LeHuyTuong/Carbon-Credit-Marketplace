package com.carbonx.marketcarbon.helper.notification.impl;

import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationServiceImpl implements ApplicationNotificationService {

    private final EmailService emailService;

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
}

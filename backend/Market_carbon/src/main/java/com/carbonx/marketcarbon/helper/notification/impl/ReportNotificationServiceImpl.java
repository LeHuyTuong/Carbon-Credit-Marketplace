package com.carbonx.marketcarbon.helper.notification.impl;

import com.carbonx.marketcarbon.helper.notification.ReportNotificationService;
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
public class ReportNotificationServiceImpl implements ReportNotificationService {

    private final EmailService emailService;

    @Async
    @Override
    public void sendCvaDecision(String email, String companyName, Long reportId,
                                String projectName, String reviewerName,
                                boolean approved, String note) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", companyName);
        vars.put("projectName", projectName);
        vars.put("reportId", reportId);
        vars.put("reviewerName", reviewerName);
        vars.put("reviewNote", note);
        vars.put("decision", approved ? "Approved by CVA" : "Rejected by CVA");
        vars.put("badgeColor", approved ? "#16a34a" : "#dc2626");
        vars.put("approved", approved);

        String subject = approved
                ? "[CarbonX] Your report was approved by CVA"
                : "[CarbonX] Your report was rejected by CVA";

        try {
            String html = emailService.renderCvaDecisionEmail(vars);
            emailService.sendHtml(email, subject, html);
        } catch (Exception e) {
            log.warn(" Failed to send CVA decision email to {}: {}", email, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendAdminDecision(String email, String companyName, Long reportId,
                                  String projectName, String reviewerName,
                                  boolean approved, String note) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", companyName);
        vars.put("projectName", projectName);
        vars.put("reportId", reportId);
        vars.put("reviewerName", reviewerName);
        vars.put("reviewNote", note);
        vars.put("decision", approved ? "Final Approval" : "Final Rejection");
        vars.put("badgeColor", approved ? "#16a34a" : "#dc2626");
        vars.put("approved", approved);

        String subject = approved
                ? "[CarbonX] Final approval granted for your report"
                : "[CarbonX] Final rejection of your report";

        try {
            String html = emailService.renderAdminDecisionEmail(vars);
            emailService.sendHtml(email, subject, html);
        } catch (Exception e) {
            log.warn(" Failed to send Admin decision email to {}: {}", email, e.getMessage());
        }
    }
}
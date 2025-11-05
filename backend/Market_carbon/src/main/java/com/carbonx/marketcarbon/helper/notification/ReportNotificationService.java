package com.carbonx.marketcarbon.helper.notification;

public interface ReportNotificationService {
    void sendCvaDecision(String email, String companyName, Long reportId,
                         String projectName, String reviewerName,
                         boolean approved, String note);

    void sendAdminDecision(String email, String companyName, Long reportId,
                           String projectName, String reviewerName,
                           boolean approved, String note);
}

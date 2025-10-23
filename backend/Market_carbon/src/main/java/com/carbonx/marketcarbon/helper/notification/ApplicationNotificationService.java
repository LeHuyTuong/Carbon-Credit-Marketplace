package com.carbonx.marketcarbon.helper.notification;

import com.carbonx.marketcarbon.model.ProjectApplication;

public interface ApplicationNotificationService {
    void sendCvaDecision(String email, String companyName, Long applicationId,
                         String projectName, String reviewerName,
                         boolean approved, String note);
    void sendAdminDecision(String email, String companyName, Long applicationId,
                           String projectName, String reviewerName,
                           boolean approved, String note);
}

package com.carbonx.marketcarbon.helper.notification;

import com.carbonx.marketcarbon.model.ProjectApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ApplicationNotificationService {
    void sendCvaDecision(String email, String companyName, Long applicationId,
                         String projectName, String reviewerName,
                         boolean approved, String note);

    void sendAdminDecision(String email, String companyName, Long applicationId,
                           String projectName, String reviewerName,
                           boolean approved, String note);

    void sendAdminConfirmRequestWithdrawal(
            String userEmail,         // Email người nhận
            String userName,
            Long withdrawalId,      // ID của yêu cầu rút tiền
            BigDecimal amount,        // Số tiền rút
            LocalDateTime requestedAt // Thời gian xử lý
    );

    void sendWithdrawalFailedOrRejected(
            String userEmail,
            String userName,
            Long withdrawalId,
            BigDecimal amount,
            String reason, // Thêm lý do
            LocalDateTime processedAt
    );
}

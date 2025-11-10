package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.model.ProjectApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApplicationResponse {

    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long companyId;
    private String companyName;
    private ApplicationStatus status;
    private String reviewNote;
    private String finalReviewNote;
    private String applicationDocsPath;
    private String applicationDocsUrl;
    private LocalDateTime submittedAt;

    // Hiển thị tên người duyệt
    private String cvaReviewerName;
    private String adminReviewerName;

    // Trạng thái chờ xử lý / hướng dẫn tiếp theo
    private String waitingFor;

    public static ProjectApplicationResponse fromEntity(ProjectApplication app) {
        String cvaName = null;
        String adminName = null;
        String waitingFor = null;

        if (app.getReviewer() != null) {
            cvaName = app.getReviewer().getDisplayName();
        }

        if (app.getFinalReviewer() != null) {
            adminName = app.getFinalReviewer().getDisplayName();
        }

        // Xác định hướng dẫn tiếp theo dựa trên trạng thái
        switch (app.getStatus()) {
            case UNDER_REVIEW ->
                    waitingFor = "Waiting for CVA review — please wait until the CVA completes the evaluation.";
            case CVA_APPROVED ->
                    waitingFor = "Waiting for Admin approval — your application has passed the CVA review and is now pending final approval from the Admin.";
            case CVA_REJECTED ->
                    waitingFor = "Rejected by CVA — please review the CVA’s feedback, make corrections, and resubmit your application.";
            case ADMIN_APPROVED ->
                    waitingFor = "Approved by Admin — your application is complete. You can now join the project and upload emission reports for credit issuance.";
            case ADMIN_REJECTED ->
                    waitingFor = "Rejected by Admin — please review the Admin’s feedback, update your documents or data, and resubmit if applicable.";
            case NEEDS_REVISION ->
                    waitingFor = "Requires revision and resubmission — please update the required sections and upload the revised documents.";
            default ->
                    waitingFor = "Unknown status — please contact system support or the CVA team for clarification.";
        }

        return ProjectApplicationResponse.builder()
                .id(app.getId())
                .projectId(app.getProject() != null ? app.getProject().getId() : null)
                .projectTitle(app.getProject() != null ? app.getProject().getTitle() : null)
                .companyId(app.getCompany() != null ? app.getCompany().getId() : null)
                .companyName(app.getCompany() != null ? app.getCompany().getCompanyName() : null)
                .status(app.getStatus())
                .reviewNote(app.getReviewNote())
                .finalReviewNote(app.getFinalReviewNote())
                .applicationDocsPath(app.getApplicationDocsPath())
                .applicationDocsUrl(app.getApplicationDocsUrl())
                .submittedAt(app.getSubmittedAt())
                .cvaReviewerName(cvaName)
                .adminReviewerName(adminName)
                .waitingFor(waitingFor)
                .build();
    }
}

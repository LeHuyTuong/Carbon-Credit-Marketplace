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

    //  Thêm hai trường hiển thị tên người duyệt
    private String cvaReviewerName;
    private String adminReviewerName;

    public static ProjectApplicationResponse fromEntity(ProjectApplication app) {
        String cvaName = null;
        String adminName = null;

        if (app.getReviewer() != null) {
            cvaName = app.getReviewer().getDisplayName();
        }

        if (app.getFinalReviewer() != null) {
            adminName = app.getFinalReviewer().getDisplayName();
        }

        return ProjectApplicationResponse.builder()
                .id(app.getId())
                .projectId(app.getProject().getId())
                .projectTitle(app.getProject().getTitle())
                .companyId(app.getCompany().getId())
                .companyName(app.getCompany().getCompanyName())
                .status(app.getStatus())
                .reviewNote(app.getReviewNote())
                .finalReviewNote(app.getFinalReviewNote())
                .applicationDocsUrl(app.getApplicationDocsUrl())
                .applicationDocsPath(app.getApplicationDocsPath())
                .submittedAt(app.getSubmittedAt())
                //  Gán thêm
                .cvaReviewerName(cvaName)
                .adminReviewerName(adminName)
                .build();
    }
}

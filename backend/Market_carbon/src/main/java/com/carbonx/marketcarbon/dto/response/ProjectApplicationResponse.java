package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.model.ProjectApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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
    private OffsetDateTime submittedAt;

    public static ProjectApplicationResponse fromEntity(ProjectApplication app) {
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
                .build();
    }
}
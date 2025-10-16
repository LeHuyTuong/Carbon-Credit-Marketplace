package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectReviewRequest {
    @NotNull
    private Long projectId;

    @NotNull
    private ProjectStatus decision; // APPROVED hoặc REJECTED

    private String reviewNote;
}

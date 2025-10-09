package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String title;
    private String description;
    private String logo;
    private ProjectStatus status;
    private String companyName;

    // Hồ sơ nộp và kết quả thẩm định
    private String commitments;
    private String technicalIndicators;
    private String measurementMethod;
    private String legalDocsUrl;
    private String reviewer;
    private String reviewNote;

    // Thời gian tạo
    private OffsetDateTime createdAt;
}
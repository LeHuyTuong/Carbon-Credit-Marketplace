package com.carbonx.marketcarbon.dto.request.importing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectCsvRow {
    private Long baseProjectId;
    private Long companyId;
    private String title;
    private String description;
    private String logo;
    private String commitments;
    private String technicalIndicators;
    private String measurementMethod;
    private String legalDocsUrl;
}
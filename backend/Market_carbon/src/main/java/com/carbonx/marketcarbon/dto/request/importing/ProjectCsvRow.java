package com.carbonx.marketcarbon.dto.request.importing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectCsvRow {
    private Long baseProjectId;
    private String companyCommitment;
    private String technicalIndicators;
    private String measurementMethod;
}
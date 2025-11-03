package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String title;
    private String description;
    private ProjectStatus status;
    private String commitments;
    private String technicalIndicators;
    private String measurementMethod;
    private String legalDocsFile;
    private String logo;
    private String createdByName;

    private LocalDate startedDate;
    private LocalDate endDate;
    private BigDecimal emissionFactorKgPerKwh;
}
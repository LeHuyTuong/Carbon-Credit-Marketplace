package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ProjectSubmitRequest {
    @NotNull
    private Long companyId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @URL
    @NotBlank
    private String logo;

    @NotBlank
    private String commitments;

    @NotBlank
    private String technicalIndicators;

    @NotBlank
    private String measurementMethod;

    private String legalDocsUrl; // có thể để trống, upload sau
}
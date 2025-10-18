package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectApplicationRequest {

    @NotNull
    private Long projectId;

    private String applicationDocsUrl;
}

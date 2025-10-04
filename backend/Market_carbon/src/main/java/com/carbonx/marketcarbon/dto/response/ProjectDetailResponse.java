package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.model.Company;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ProjectDetailResponse {

    private Long id;
    private String title;
    private String description;
    private String logo;
    private Status status;
    private Company company;
    private OffsetDateTime createAt;
    private OffsetDateTime updatedAt;
}

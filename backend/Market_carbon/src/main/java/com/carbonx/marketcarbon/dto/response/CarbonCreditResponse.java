package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.CarbonCredit;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record CarbonCreditResponse(
        Long id,
        String creditCode,
        String status,
        Long projectId,
        String projectTitle,
        Long companyId,
        String companyName,
        Integer vintageYear,
        String batchCode,
        OffsetDateTime issuedAt
) {
    public static CarbonCreditResponse from(CarbonCredit c) {
        return CarbonCreditResponse.builder()
                .id(c.getId())
                .creditCode(c.getCreditCode())
                .status(c.getStatus().name())
                .projectId(c.getProject() != null ? c.getProject().getId() : null)
                .projectTitle(c.getProject() != null ? c.getProject().getTitle() : null)
                .companyId(c.getCompany() != null ? c.getCompany().getId() : null)
                .companyName(c.getCompany() != null ? c.getCompany().getCompanyName() : null)
                .vintageYear(c.getBatch() != null ? c.getBatch().getVintageYear() : null)
                .batchCode(c.getBatch() != null ? c.getBatch().getBatchCode() : null)
                .issuedAt(c.getIssuedAt())
                .build();
    }
}

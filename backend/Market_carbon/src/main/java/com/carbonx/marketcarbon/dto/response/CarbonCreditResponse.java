package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.CarbonCredit;
import lombok.Builder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

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
        OffsetDateTime issuedAt,
        LocalDate expiryDate
) {
    public static CarbonCreditResponse from(CarbonCredit c) {
        LocalDate expiry = c.getExpiryDate();
        Long daysRemaining = null;

        if (expiry != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        }

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
                .expiryDate(expiry)
                .build();
    }
}

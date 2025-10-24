package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.CreditBatch;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record CreditBatchLiteResponse(
        Long id,
        String batchCode,
        Integer vintageYear,
        Long projectId,
        String projectTitle,
        Integer creditsCount,
        BigDecimal totalTco2e,
        BigDecimal residualTco2e,
        String status,
        OffsetDateTime issuedAt
) {
    public static CreditBatchLiteResponse from(CreditBatch b) {
        return CreditBatchLiteResponse.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .vintageYear(b.getVintageYear())
                .projectId(b.getProject() != null ? b.getProject().getId() : null)
                .projectTitle(b.getProject() != null ? b.getProject().getTitle() : null)
                .creditsCount(b.getCreditsCount())
                .totalTco2e(b.getTotalTco2e())
                .residualTco2e(b.getResidualTco2e())
                .status(b.getStatus())
                .issuedAt(b.getIssuedAt())
                .build();
    }
}

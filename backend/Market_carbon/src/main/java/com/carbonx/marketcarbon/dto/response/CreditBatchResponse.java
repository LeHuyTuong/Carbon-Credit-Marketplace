package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.CreditBatch;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditBatchResponse {
    Long id;
    String batchCode;
    Integer vintageYear;
    Integer creditsCount;
    BigDecimal totalTco2e;
    BigDecimal residualTco2e;
    Long serialFrom;
    Long serialTo;
    String companyName;
    String projectTitle;
    String status;
    LocalDateTime issuedAt;

    String certificateUrl;

    private Long reportId;
    private String reportPeriod;


    public static CreditBatchResponse from(CreditBatch entity) {
        return CreditBatchResponse.builder()
                .id(entity.getId())
                .batchCode(entity.getBatchCode())
                .vintageYear(entity.getVintageYear())
                .creditsCount(entity.getCreditsCount())
                .totalTco2e(entity.getTotalTco2e())
                .residualTco2e(entity.getResidualTco2e())
                .serialFrom(entity.getSerialFrom())
                .serialTo(entity.getSerialTo())
                .companyName(entity.getCompany() != null ? entity.getCompany().getCompanyName() : null)
                .projectTitle(entity.getProject() != null ? entity.getProject().getTitle() : null)
                .status(entity.getStatus())
                .issuedAt(entity.getIssuedAt())
                .certificateUrl(entity.getCertificate() != null ? entity.getCertificate().getCertificateUrl() : null)
                .reportId(entity.getReport() != null ? entity.getReport().getId() : null)
                .reportPeriod(entity.getReport() != null ? entity.getReport().getPeriod() : null)
                .build();
    }
}
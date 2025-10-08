package com.carbonx.marketcarbon.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmissionReportResponse {
    private Long id;
    private String period;
    private BigDecimal calculatedCo2;
    private BigDecimal baselineIceCo2;
    private BigDecimal evCo2;
    private String status;
    private String sellerEmail;
    private String sellerCompanyName;
    private String vehiclePlate;
    private OffsetDateTime createdAt;
    private OffsetDateTime submittedAt;

    private String csvUrl;
}

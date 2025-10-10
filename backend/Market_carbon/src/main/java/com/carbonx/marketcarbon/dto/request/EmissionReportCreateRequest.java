package com.carbonx.marketcarbon.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmissionReportCreateRequest {
    Long vehicleId;
    String period;
    BigDecimal calculatedCo2;
    BigDecimal baselineIceCo2;
    BigDecimal evCo2;
}
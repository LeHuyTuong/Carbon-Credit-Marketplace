package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.EmissionReportDetail;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmissionReportDetailResponse {
    private Long id;
    private Long vehicleId;
    private String period;
    private BigDecimal totalEnergy;
    private BigDecimal co2Kg;

    public static EmissionReportDetailResponse from(EmissionReportDetail d) {
        return EmissionReportDetailResponse.builder()
                .id(d.getId())
                .vehicleId(d.getVehicleId())
                .period(d.getPeriod())
                .totalEnergy(d.getTotalEnergy())
                .co2Kg(d.getCo2Kg())
                .build();
    }
}

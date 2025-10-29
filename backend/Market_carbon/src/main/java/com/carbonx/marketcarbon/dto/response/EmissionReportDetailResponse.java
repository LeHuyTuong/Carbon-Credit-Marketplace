package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmissionReportDetailResponse {
    private Long id;
    private String vehiclePlate;
    private String period;
    private BigDecimal totalEnergy;
    private BigDecimal co2Kg;

    @JsonProperty("vehicleId")
    public String getVehicleId() {
        return vehiclePlate;
    }

    public static EmissionReportDetailResponse from(EmissionReportDetail d) {
        return EmissionReportDetailResponse.builder()
                .id(d.getId())
                .period(d.getPeriod())
                .totalEnergy(d.getTotalEnergy())
                .co2Kg(d.getCo2Kg())
                .vehiclePlate(d.getVehiclePlate())
                .build();
    }
}

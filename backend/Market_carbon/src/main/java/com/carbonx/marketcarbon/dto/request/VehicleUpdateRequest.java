package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleUpdateRequest {

    @NotNull(message = "plateNumber can not null ")
    @PlateNumber
    private String plateNumber;
    @NotNull(message = "model can not null ")
    private String model;
    @NotNull(message = "brand can not null ")
    private String brand;
    @NotNull(message = "companyId can not null ")
    private Long companyId;
}

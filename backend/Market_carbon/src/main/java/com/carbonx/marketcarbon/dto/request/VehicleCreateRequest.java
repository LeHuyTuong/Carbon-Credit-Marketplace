package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleCreateRequest {
    private Long ownerId;

    @NotNull(message = "plateNumber is not null")
    @PlateNumber
    private String plateNumber;

    @NotNull(message = "model is not null")
    private String model;

    @NotNull(message = "brand is not null")
    private String brand;

    @NotNull(message = "companyId  is not null")
    private Long company;

}

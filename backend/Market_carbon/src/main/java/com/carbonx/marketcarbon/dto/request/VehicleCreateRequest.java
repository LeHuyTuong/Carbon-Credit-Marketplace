package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleCreateRequest {

    @NotNull(message = "plateNumber is not null")
    @PlateNumber(message = "plateNumber is not fit format : 50H-2228")
    private String plateNumber;

    @NotNull(message = "model is not null")
    private String model;

    @NotNull(message = "brand is not null")
    private String brand;

    @NotNull(message = "companyId  is not null")
    private Long companyId;

}

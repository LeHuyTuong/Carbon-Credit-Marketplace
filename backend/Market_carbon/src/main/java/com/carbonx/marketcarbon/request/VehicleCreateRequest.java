package com.carbonx.marketcarbon.request;

import com.carbonx.marketcarbon.domain.annotation.PlateNumber;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleCreateRequest {
    private Long ownerId;

    @NotNull(message = "plateNummber")
    @PlateNumber
    private String plateNumber;

    @NotNull(message = "model is not null")
    private String model;

    @NotNull(message = "brand is not null")
    private String brand;

    @NotNull(message = "manufacturer is not null")
    private String manufacturer;
    private Integer YearOfManufacture;

    @NotNull(message = "year of manufacture is not null")
    private Integer yearOfManufacture;
}

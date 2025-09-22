package com.carbonx.marketcarbon.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponse {
    private Long id;
    private Long ownerId;
    private String plateNumber;
    private String brand;
    private String model;
    private Integer yearOfManufacture;
}


package com.carbonx.marketcarbon.dto.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {
    private String plateNumber;
    private String model;
    private String brand;
    private String manufacturer;
    private Integer year;
}

package com.carbonx.marketcarbon.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {
    private Long id;
    private String plateNumber;
    private String model;
    private String brand;
    private String manufacturer;
    private Integer year;
}

package com.carbonx.marketcarbon.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {
    private String plateNumber;
    private String model;
    private String manufacturer;
    private Integer year;
}
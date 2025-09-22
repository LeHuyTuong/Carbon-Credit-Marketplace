package com.example.Market_carbon.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {
    private String plateNumber;
    private String model;
    private String manufacturer;
    private Integer year;
}
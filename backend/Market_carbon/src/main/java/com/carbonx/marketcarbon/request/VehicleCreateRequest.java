package com.carbonx.marketcarbon.request;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class VehicleCreateRequest {
    private String plateNumber;
    private String model;
    private String brand;
    private String manufacturer;
    private Integer year;
}

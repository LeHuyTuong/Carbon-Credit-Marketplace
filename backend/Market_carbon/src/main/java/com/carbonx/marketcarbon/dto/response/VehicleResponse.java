package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.Vehicle;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponse {
    private Long id;
    private String plateNumber;
    private String brand;
    private String model;
    private Long companyId;

    //VehicleResponse.from(Vehicle vehicle)
    // là static factory method để map 1 entity Vehicle → DTO VehicleResponse bằng builder.
    public static VehicleResponse from(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .companyId(vehicle.getCompany() != null ? vehicle.getCompany().getId() : null)
                .build();
    }
    //Cắt vòng lặp serialize (không trả nguyên Company) và
    // kiểm soát payload: chỉ expose id, plateNumber, companyId, model.

}


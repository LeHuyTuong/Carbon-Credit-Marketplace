package com.carbonx.marketcarbon.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyVehicleSummaryResponse {
    private Long ownerId;
    private String fullName;
    private String email;
    private long vehicleCount;
    private List<VehicleItem> vehicles;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleItem {
        private Long id;
        private String plateNumber;
        private String brand;
        private String model;
    }
}

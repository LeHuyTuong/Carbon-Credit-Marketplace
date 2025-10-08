package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@Builder
public class VehicleDetailResponse {
    private Long id;
    private String plateNumber;
    private String brand;
    private String model;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long companyId;
    //TODO sau nay se co them so tin chi carbon ma moi xe co , chi cho thang Aggree biest
    // va so tien ma EV Owner dc nhan tu xe cua ho
}

package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VehicleUpdateRequest {

    @NotNull(message = "plateNumber is not null")
    @PlateNumber(message = "plateNumber is not fit format : 50H-2228")
    private String plateNumber;

    @NotNull(message = "model can not null ")
    private String model;

    @NotNull(message = "brand can not null ")
    private String brand;

    @NotNull(message = "companyId can not null ")
    private Long companyId;

    private MultipartFile documentFile;
}

package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VehicleCreateRequest {

    @NotBlank(message = "Plate number must not be blank")
    @PlateNumber(message = "plateNumber is not fit format : 50H-2228 , 30A-1234A , 29-AA-12345,29-S1-12345, 51A-123.45 ")
    private String plateNumber;

    @NotBlank(message = "Brand must not be blank")
    private String brand;

    @NotBlank(message = "Model must not be blank")
    private String model;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Document file is required")
    private MultipartFile documentFile;

}

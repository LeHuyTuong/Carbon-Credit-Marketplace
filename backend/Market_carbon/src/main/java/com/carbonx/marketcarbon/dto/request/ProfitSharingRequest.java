package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProfitSharingRequest {

    //  Chia lợi nhuận từ một dự án cụ thể
    private Long projectId;

    //  Chỉ chia lợi nhuận cho 1 report cụ thể
    private Long emissionReportId;

    @NotNull(message = "Mô tả không được để trống")
    private String description;

}

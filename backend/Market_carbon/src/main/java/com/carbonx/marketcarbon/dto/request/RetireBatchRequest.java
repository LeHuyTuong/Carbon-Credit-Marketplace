package com.carbonx.marketcarbon.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetireBatchRequest {
    /**
     * Batch Code mà người dùng nhìn thấy trên UI
     * VD: "2026-LAM3-GRE1-000001_000100"
     */
    @NotBlank(message = "Batch code is required")
    private String batchCode;

    @NotNull(message = "Retire quantity is required")
    @Positive(message = "Retire quantity must be greater than zero")
    private BigDecimal quantity;
}

package com.carbonx.marketcarbon.dto.request;


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
    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Retire quantity is required")
    @Positive(message = "Retire quantity must be greater than zero")
    private BigDecimal quantity;
}

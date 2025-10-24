package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderRequest {

    @NotNull(message = "buyerCompanyId cannot be null")
    private Long buyerCompanyId;

    @NotNull(message = "listingId cannot be null")
    private Long listingId;

    @NotNull(message = "quantity cannot be null")
    private BigDecimal quantity;
}

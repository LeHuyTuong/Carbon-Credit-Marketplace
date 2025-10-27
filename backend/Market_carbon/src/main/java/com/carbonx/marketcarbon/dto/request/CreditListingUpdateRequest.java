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
public class CreditListingUpdateRequest {

    @NotNull(message = "Listing ID cannot be null")
    private Long listingId;

    @NotNull(message = "Price per credit cannot be null")
    @Positive(message = "Price per credit must be positive")
    private BigDecimal pricePerCredit;
}

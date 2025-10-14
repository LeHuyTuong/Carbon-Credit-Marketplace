package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class CreditListingRequest {

    @NotNull(message = "Carbon Credit ID cannot be null")
    private Long carbonCreditId;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price per credit cannot be null.")
    @Positive(message = "Price must be positive.")
    private BigDecimal pricePerCredit;


    @NotNull(message = "Expiration date cannot be null.")
    @Future(message = "Expiration date must be in the future.")
    private LocalDateTime expirationDate;
}

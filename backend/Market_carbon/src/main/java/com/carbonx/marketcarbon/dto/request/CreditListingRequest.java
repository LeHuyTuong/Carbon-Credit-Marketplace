package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class CreditListingRequest {

    private Long carbonCreditId;

    private Long batchId;

    private List<Long> carbonCreditIds;


    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price per credit cannot be null.")
    @Positive(message = "Price must be positive.")
    private BigDecimal pricePerCredit;


}

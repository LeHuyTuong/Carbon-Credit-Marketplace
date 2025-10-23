package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CvaVerificationRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
        BigDecimal verificationScore,

        @NotBlank
        String comment,

        @NotNull
        Boolean approved
) {}

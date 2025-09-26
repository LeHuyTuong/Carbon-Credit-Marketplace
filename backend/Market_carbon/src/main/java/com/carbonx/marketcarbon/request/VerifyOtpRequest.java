package com.carbonx.marketcarbon.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class VerifyOtpRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otpCode;
}


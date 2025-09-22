package com.example.Market_carbon.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycRequest {
    @NotNull
    private Long userId;
    private String phone;
    private String country;
    private String documentType;
    private String documentNumber;
    private String documentUrl;
}
package com.carbonx.marketcarbon.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentDetailsRequest {
    @NotNull
    private String accountNumber; // STK
    @NotNull
    private String accountHolderName; // tên chủ tk
    @NotNull
    private String bankCode; // VCB/TCB/ACB
    @NotNull
    private String customerName;
}

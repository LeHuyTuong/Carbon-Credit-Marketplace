package com.carbonx.marketcarbon.dto.request;


import lombok.Data;

@Data
public class PaymentDetailsRequest {
    private String accountNumber; // STK
    private String accountHolderName; // tên chủ tk
    private String bankCode; // VCB/TCB/ACB
    private String customerName;
}

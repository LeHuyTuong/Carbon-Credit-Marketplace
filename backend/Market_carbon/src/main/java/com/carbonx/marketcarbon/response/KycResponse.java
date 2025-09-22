package com.carbonx.marketcarbon.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycResponse {
    private Long id;
    private Long userId;
    private String phone;
    private String country;
    private String kycStatus;
    private String documentType;
    private String documentNumber;
    private String documentUrl;
}


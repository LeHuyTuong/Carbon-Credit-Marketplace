package com.carbonx.marketcarbon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;


@Data
@Builder
@AllArgsConstructor
public class KycCompanyResponse {
    private Long id;
    private String businessLicense;
    private String taxCode;
    private String companyName;
    private String address;
    OffsetDateTime createAt;
    OffsetDateTime updatedAt;
}

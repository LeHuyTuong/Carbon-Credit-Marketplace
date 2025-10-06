package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class KycCompanyResponse {
    private Long id;
    private Long company;
    private String businessLicense;
    private String taxCode;
    private String companyName;
    private String address;
}

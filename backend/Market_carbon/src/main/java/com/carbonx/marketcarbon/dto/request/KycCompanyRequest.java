package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class KycCompanyRequest {

    @NotNull(message = "business license is not null")
    private String businessLicense;

    @NotNull(message = "tax code is not null")
    private String taxCode;

    @NotNull(message = "company name is not null")
    private String companyName;

    @NotNull(message = "address is not null")
    private String address;

    public interface Create{} // tách riêng , email được create
    public interface Update{} // update ko được đổi email
}

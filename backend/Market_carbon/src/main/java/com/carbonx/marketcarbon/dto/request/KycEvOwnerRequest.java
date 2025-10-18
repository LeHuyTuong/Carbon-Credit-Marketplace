package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.annotation.DocumentNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycEvOwnerRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Phone must not be blank")
    private String phone;

    @NotBlank(message = "Country must not be blank")
    private String country;

    @NotBlank(message = "Address must not be blank")
    private String address;

    @NotNull(message = "Birth date must not be null")
    private LocalDate birthDate;

    @NotNull(message = "Document type must not be null")
    private IDType documentType;

    @NotBlank(message = "Document number must not be blank")
    @DocumentNumber(message = "Document number invalid format")
    private String documentNumber;

    @NotNull(message = "Gender must not be null")
    private Gender gender;
}
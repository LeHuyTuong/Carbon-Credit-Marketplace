package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.annotation.PhoneNumber;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;

import java.time.LocalDate;

@Data
public class KycRequest {

    @NotNull(message = "name is not null")
    private String name;

    @PhoneNumber(message = "phone invalid format")
    private String phone;

    private Gender gender;

    @NotNull(message = "country is not null")
    private String country;

    @NotEmpty(message = "address is not null")
    private String address;

    @NotNull(message = "documentType is not null")
    private IDType documentType;

    @NotEmpty(message = "documentNumber is not empty ")
    private String documentNumber;

    @NotNull(groups = {Create.class, Update.class})
    @JsonAlias({"birthDate", "birthday"})
    @JsonFormat(pattern = "yyyy-MM-dd")// parse date
    private LocalDate birthDate;


    public interface Create{} // tách riêng , email được create
    public interface Update{} // update ko được đổi email
}

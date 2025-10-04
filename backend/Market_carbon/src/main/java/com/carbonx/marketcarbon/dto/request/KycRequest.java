package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.annotation.PhoneNumber;
import jakarta.validation.constraints.Email;
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

    @NotNull(message = "birthday is not null")
    private LocalDate birthday;

    public interface Create{} // tách riêng , email được create
    public interface Update{} // update ko được đổi email
}

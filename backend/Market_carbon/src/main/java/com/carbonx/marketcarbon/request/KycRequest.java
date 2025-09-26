package com.carbonx.marketcarbon.request;

import com.carbonx.marketcarbon.domain.IDType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;

import java.sql.Date;
import java.time.LocalDate;

@Data
public class KycRequest {
    @NotNull(groups = CreatedBy.class)
    private Long userId;
    @Email @NotBlank(groups = CreatedBy.class)
    private String email;
    private String name;
    private String phone;
    private String country;
    private String address;
    private IDType documentType;
    private String documentNumber;
    private LocalDate birthday;

    public interface Create{} // tách riêng , email được create
    public interface Update{} // update ko được đổi email
}

package com.carbonx.marketcarbon.request;

import com.carbonx.marketcarbon.domain.USER_ROLE;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    private String email;
    private String password;   // tên khớp JSON
    private String fullName;
    private USER_ROLE role;
}

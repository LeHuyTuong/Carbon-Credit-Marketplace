package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.USER_ROLE;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String message;
    private String jwt;
    private USER_ROLE role;

}


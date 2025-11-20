package com.carbonx.marketcarbon.dto.request;


import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeUserStatusRequest {

    @NotNull
    private USER_STATUS status;
}
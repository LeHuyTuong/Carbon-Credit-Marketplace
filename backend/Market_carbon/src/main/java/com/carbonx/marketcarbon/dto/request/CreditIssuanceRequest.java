package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreditIssuanceRequest {

    @NotEmpty(message = "List of charging data IDs cannot be empty.")
    private List<Long> chargingDataIds;

    @NotNull(message = "Project ID cannot be null")
    private Long projectId;
}

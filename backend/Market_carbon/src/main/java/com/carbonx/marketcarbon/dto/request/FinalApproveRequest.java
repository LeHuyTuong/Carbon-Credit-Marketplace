package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.ProjectStatus;
import lombok.Data;

@Data
public class FinalApproveRequest {
    private String reviewer;
    private ProjectStatus status;
}

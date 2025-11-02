package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tóm tắt cho một đợt chia sẻ lợi nhuận (dùng trong danh sách).
 */
@Data
@Builder
public class ProfitDistributionResponse {
    private Long id;
    private LocalDateTime createdAt;
    private BigDecimal totalMoneyDistributed;
    private ProfitDistributionStatus status;
    private String description;
    private Long projectId;
    private String projectName;
}


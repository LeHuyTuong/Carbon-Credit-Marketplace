package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chi tiết đầy đủ cho một đợt chia sẻ lợi nhuận.
 */
@Data
@Builder
public class ProfitDistributionDetailResponse {
    // Thông tin tổng
    private Long id;
    private LocalDateTime createdAt;
    private BigDecimal totalMoneyDistributed;
    private ProfitDistributionStatus status;
    private String description;
    private Long projectId;
    private String projectName;

    // Danh sách chi tiết
    private List<ProfitDistributionDetailItemResponse> details;
}

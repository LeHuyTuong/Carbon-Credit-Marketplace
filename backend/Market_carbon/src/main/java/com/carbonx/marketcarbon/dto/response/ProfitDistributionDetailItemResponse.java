package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO cho một dòng chi tiết trong đợt chia sẻ lợi nhuận (cho 1 EV Owner).
 */
@Data
@Builder
public class ProfitDistributionDetailItemResponse {
    private Long evOwnerId;
    private String evOwnerName; // Tên của chủ xe
    private BigDecimal moneyAmount;
    private String status;
    private String errorMessage;
}

package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO tóm tắt số lượng tín chỉ đã retired và tổng tín chỉ active
 */
@Data
@Builder
public class WalletRetiredSummaryResponse {
    private BigDecimal totalRetired;

    /**
     * Tổng số tín chỉ đang hoạt động (AVAILABLE + LISTED)
     */
    private BigDecimal totalActive;
}

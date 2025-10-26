package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransactionResponse {
    private Long id;

    // Chỉ lấy orderId thay vì cả object Order
    private Long orderId;

    private WalletTransactionType transactionType;
    private String description;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private String batchCode;

}

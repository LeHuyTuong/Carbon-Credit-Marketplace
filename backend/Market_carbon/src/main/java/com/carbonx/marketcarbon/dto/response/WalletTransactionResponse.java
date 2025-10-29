package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.CarbonCredit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    // Số lượng tín chỉ carbon giao dịch (giúp người dùng đối chiếu với amount)
    private BigDecimal carbonCreditQuantity;
    // Đơn giá / tín chỉ nếu transaction gắn với order
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    private String batchCode;

    private CarbonCredit carbonCredit;
}

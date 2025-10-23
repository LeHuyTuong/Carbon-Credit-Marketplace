package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WalletResponse {
    private Long id;

    // Chỉ lấy userId thay vì cả object User
    private Long userId;

    private BigDecimal balance;
    private BigDecimal carbonCreditBalance;

    // Thay thế List<WalletTransaction> bằng List<WalletTransactionResponse>
    private List<WalletTransactionResponse> walletTransactions;
}

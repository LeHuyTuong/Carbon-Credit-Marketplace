package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Value
public class WalletResponse {
     Long id;

    // Chỉ lấy userId thay vì cả object User
     Long userId;

     BigDecimal balance;
     BigDecimal carbonCreditBalance;

     List<WalletTransactionResponse> walletTransactions;

     List<WalletCarbonCreditResponse> carbonCredits;
}

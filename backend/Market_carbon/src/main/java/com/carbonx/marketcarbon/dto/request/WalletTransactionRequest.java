package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Wallet;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletTransactionRequest {
    private Wallet wallet;
    private WalletTransactionType type;
    private String transfer;
    private String purpose;
    private Long amount;
}

package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Wallet;
import lombok.Data;

@Data
public class WalletTransactionRequest {
    private Wallet wallet;
    private WalletTransactionType type;
    private String transfer;
    private String purpose;
    private Long Amount;

    public WalletTransactionRequest(Wallet wallet, WalletTransactionType type) {
        this.wallet = wallet;
        this.type = type;
    }

    public WalletTransactionRequest(Wallet wallet, WalletTransactionType type, String transfer, String purpose, Long amount) {
        this.wallet = wallet;
        this.type = type;
        this.transfer = transfer;
        this.purpose = purpose;
        Amount = amount;
    }
}

package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;

import java.util.List;

public interface WalletTransactionService {
    WalletTransaction createTransaction(WalletTransactionRequest request);
    List<WalletTransaction> getTransaction(Wallet wallet, WalletTransactionType type);
}

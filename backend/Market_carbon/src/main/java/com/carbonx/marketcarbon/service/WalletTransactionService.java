package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.model.WalletTransaction;

import java.util.List;

public interface WalletTransactionService {
    // Return DTO after creation
    WalletTransactionResponse createTransaction(WalletTransactionRequest request);

    // Return list of DTOs for API
    List<WalletTransactionResponse> getTransactions();

    // New method to get DTOs specifically for a wallet ID
    List<WalletTransactionResponse> getTransactionDtosForWallet(Long walletId);

    long countMyTransactions(); // Đếm giao dịch của người dùng hiện tại
    long countAllTransactions(); // Đếm tất cả giao dịch (cho Admin)

}

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public WalletTransaction createTransaction(WalletTransactionRequest request) {

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(request.getWallet());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setTransferId(request.getTransfer());
        transaction.setPurpose(request.getPurpose());
        transaction.setAmount(request.getAmount());

        return walletTransactionRepository.save(transaction);
    }

    @Override
    public List<WalletTransaction> getTransaction(Wallet wallet, WalletTransactionType type) {
        return walletTransactionRepository.findByWalletOrderByDateDesc(wallet);
    }
}

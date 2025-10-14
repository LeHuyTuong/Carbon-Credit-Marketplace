package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        transaction.setCreateAt(OffsetDateTime.now());
        transaction.setUpdatedAt(OffsetDateTime.now());

        return walletTransactionRepository.save(transaction);
    }

    @Override
    public List<WalletTransaction> getTransaction(Wallet wallet, WalletTransactionType type) {
        return walletTransactionRepository.findByWalletOrderByCreateAtDesc(wallet);
    }
}

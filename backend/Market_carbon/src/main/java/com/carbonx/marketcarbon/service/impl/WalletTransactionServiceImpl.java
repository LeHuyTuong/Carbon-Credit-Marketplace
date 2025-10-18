package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public WalletTransaction createTransaction(WalletTransactionRequest request) {

        User currentUser = currentUser();
        Wallet wallet = walletRepository.findByUserId(currentUser.getId());
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet cannot be null in WalletTransactionRequest");
        }
        BigDecimal amount = request.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null in WalletTransactionRequest");
        }
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(request.getWallet());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setOrder(request.getOrder());

        BigDecimal balanceBefore = wallet.getBalance();

        BigDecimal balanceAfter;
        if (request.getType() == WalletTransactionType.WITH_DRAWL) {
            balanceAfter = balanceBefore.subtract(amount);
        } else {
            balanceAfter = balanceBefore.add(amount);
        }

        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);

        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient balance for transaction");
        }
        
        return walletTransactionRepository.save(transaction);
    }

    @Override
    public List<WalletTransaction> getTransaction() {
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            // Nếu user chưa có ví, trả về danh sách rỗng
            return List.of();
        }
        return walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }

}

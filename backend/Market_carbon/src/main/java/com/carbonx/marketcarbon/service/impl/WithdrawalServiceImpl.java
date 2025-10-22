package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.WithdrawalService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Transactional
    @Override
    public Withdrawal requestWithdrawal(Long amount) {
        User user = currentUser();
        Wallet wallet =  walletRepository.findByUserId(user.getId());
        if(amount < 10 ){
            throw new AppException(ErrorCode.WITHDRAWAL_MONEY_INVALID_AMOUNT);
        }
        //B1 tạo request withdrawal

        BigDecimal withdrawalAmount = BigDecimal.valueOf(amount);

        if(wallet.getBalance().compareTo(withdrawalAmount) >= 0 ){
            Withdrawal withdrawal = Withdrawal.builder()
                    .amount(withdrawalAmount)
                    .status(Status.PENDING)
                    .requestedAt(LocalDateTime.now())
                    .user(user)
                    .build();
            return withdrawalRepository.save(withdrawal);
        }else{
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }
    }

    @Override
    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) throws Exception {
        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);

        if(withdrawal.isEmpty()){
            throw new ResourceNotFoundException("Withdrawal not found with id: " + withdrawalId);
        }

        Withdrawal withdrawalRequest = withdrawal.get();
        withdrawalRequest.setProcessedAt(LocalDateTime.now());
        if (accept) {
            User user = withdrawalRequest.getUser();
            Wallet wallet = walletRepository.findByUserId(user.getId());
            BigDecimal amountToWithdraw = withdrawalRequest.getAmount();

            if (wallet.getBalance().compareTo(amountToWithdraw) < 0) {
                withdrawalRequest.setStatus(Status.FAILED);
                withdrawalRepository.save(withdrawalRequest);
                throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
            }

            walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                    .wallet(wallet)
                    .type(WalletTransactionType.WITH_DRAWL)
                    .description("Bank account withdrawal approved. ID: " + withdrawalId)
                    .amount(amountToWithdraw)
                    .build());

            withdrawalRequest.setStatus(Status.SUCCEEDED);
        } else {
            withdrawalRequest.setStatus(Status.REJECTED);
        }

        return withdrawalRepository.save(withdrawalRequest);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory() {
        User user = currentUser();
        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAll();
    }
}

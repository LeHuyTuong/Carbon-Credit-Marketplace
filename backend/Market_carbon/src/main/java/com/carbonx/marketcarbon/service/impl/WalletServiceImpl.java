package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.utils.CurrencyConverter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
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
    public Wallet generateWallet(User user) {
        //B1 create wallet
        Wallet wallet = new  Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCarbonCreditBalance(BigDecimal.ZERO);
        //B2 Set user , moi vi duoc 1 user
        wallet.setUser(user);
        //B3 luu lai vi theo user
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public WalletResponse getUserWallet() throws WalletException {
        // B1 Tim wallet
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        //B2 nếu không có ví thì sẽ tự gen ra ví
        if (wallet == null) {
            wallet = generateWallet(user);
        }
        //B3 khong co thi gen wallet
        // Fetch updated transactions DTOs
        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(wallet.getId());
        // Map entity to DTO
        return mapToWalletResponse(wallet, transactionDtos);
    }

    @Override
    public WalletResponse addBalanceToWallet( Long money) throws WalletException {
        // 1lấy số tiền hiện tại đang có trong ví
        User user = currentUser();
        Long id = user.getId();

        Wallet wallet = walletRepository.findByUserId(id);
        if(wallet == null){
            wallet = generateWallet(user);
        }

        // Assuming 'money' is in USD cents (e.g., 1000 for $10.00)
        BigDecimal amountUsd = BigDecimal.valueOf(money)
                .setScale(4, BigDecimal.ROUND_HALF_UP);        // Convert cents to dollars
        BigDecimal amountToAddVnd = CurrencyConverter.usdToVnd(amountUsd); // Convert USD to VND

        String description = String.format("Add money to wallet (USD %s -> VND %s)",
                amountUsd.toPlainString(), amountToAddVnd.toPlainString());

        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                .wallet(wallet)
                .amount(amountUsd)
                .type(WalletTransactionType.ADD_MONEY)
                .description(description)
                .build());

        Wallet updatedWallet = walletRepository.findById(wallet.getId())
                .orElseThrow(() -> new WalletException("Wallet disappeared after transaction")); // Should not happen in transaction

        log.info("Balance added to wallet {}. Amount USD: {}, Amount VND: {}. New Balance: {}",
                wallet.getId(), amountUsd, amountToAddVnd, updatedWallet.getBalance());

        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(updatedWallet.getId());
        // Map updated entity to DTO
        return mapToWalletResponse(updatedWallet, transactionDtos);
    }


    @Override
    @Transactional
    public WalletResponse findWalletById(Long id) throws WalletException {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletException("Wallet not found with id: " + id));
        // Fetch transaction DTOs for the wallet
        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(wallet.getId());
        // Map entity to DTO
        return mapToWalletResponse(wallet, transactionDtos);
    }

    @Override
    @Transactional // Read-only transaction for finding entity
    public Wallet findWalletEntityById(Long id) throws WalletException {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletException("Wallet entity not found with id: " + id));
    }

    // Helper method to map Wallet entity to WalletResponse DTO
    private WalletResponse mapToWalletResponse(Wallet wallet, List<WalletTransactionResponse> transactions) {
        if (wallet == null) {
            return null;
        }
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser() != null ? wallet.getUser().getId() : null)
                .balance(wallet.getBalance())
                .carbonCreditBalance(wallet.getCarbonCreditBalance())
                .walletTransactions(transactions) // Use the passed DTO list
                .build();
    }
}

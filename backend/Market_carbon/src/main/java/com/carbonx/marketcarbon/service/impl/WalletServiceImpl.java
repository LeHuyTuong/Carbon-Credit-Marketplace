package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

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


    public Wallet generateWallet(User user) {
        //B1 create wallet
        Wallet wallet = new  Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        //B2 Set user , moi vi duoc 1 user
        wallet.setUser(user);
        //B3 luu lai vi theo user
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet getUserWallet() throws WalletException {
        // B1 Tim wallet
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        //B2 Thay thi tra ve wallet
        if (wallet != null) {
            return wallet;
        }
        //B3 khong co thi gen wallet
        wallet = generateWallet(user);
        return wallet;
    }

    @Override
    public Wallet addBalanceToWallet( Long money) throws WalletException {
        // 1lấy số tiền hiện tại đang có trong ví
        User user = currentUser();
        Long id = user.getId();
        Wallet wallet = walletRepository.findByUserId(id);

        if(wallet == null){
            wallet = generateWallet(user);
        }

        BigDecimal amountToAdd = BigDecimal.valueOf(money);

        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                        .wallet(wallet)
                        .amount(amountToAdd)
                        .type(WalletTransactionType.ADD_MONEY)
                        .description("Add money to wallet")
                .build());

        Wallet updatedWallet = walletRepository.findByUserId(id);

        log.info("Wallet added to wallet" + wallet + " money :" + money);
        return updatedWallet;
    }

    @Override
    public Wallet findWalletById(Long id) throws WalletException {
        Optional<Wallet> wallet = walletRepository.findById(id);
        if(wallet.isPresent()) {
            return wallet.get();
        }
        throw new WalletException("Wallet not found" + id);
    }
}

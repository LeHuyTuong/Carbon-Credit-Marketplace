package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    public Wallet generateWallet(User user) {
        //B1 create wallet
        Wallet wallet = new  Wallet();
        //B2 Set user , moi vi duoc 1 user
        wallet.setUser(user);
        //B3 luu lai vi theo user
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet getUserWallet(User user) throws WalletException {
        // B1 Tim wallet
        Wallet wallet = findWalletById(user.getId());
        //B2 Thay thi tra ve wallet
        if (wallet != null) {
            return wallet;
        }
        //B3 khong co thi gen wallet
        wallet = generateWallet(user);
        return wallet;
    }

    @Override
    public Wallet addBalanceToWallet(Wallet wallet, Long money) throws WalletException {
        // lấy số tiền hiện tại đang có trong ví
        wallet.getBalance().add(BigDecimal.valueOf(money));

        // thêm tiền
        wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(money)));
        // lwuu thông tin
        walletRepository.save(wallet);
        log.info("Wallet added to wallet" + wallet + " money :" + money);
        return wallet;
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

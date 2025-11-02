package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletService {

    // Hiện Ví
    WalletResponse getUserWallet() throws WalletException;

    // nạp tiền vào ví
    @Transactional
    WalletResponse addBalanceToWallet(Long amount) throws WalletException;

    // tìm ví
    WalletResponse findWalletById(Long id) throws WalletException;

    // Giữ lại phương thức trả về Entity nếu cần dùng nội bộ (optional)
    Wallet findWalletEntityById(Long id) throws WalletException;

    Wallet findWalletByUser(User user);

    void transferFunds(Wallet fromWallet, Wallet toWallet, BigDecimal amount, String type, String description) throws  WalletException;
    }

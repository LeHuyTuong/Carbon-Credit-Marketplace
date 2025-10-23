package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;

import java.util.Optional;

public interface WalletService {

    // Hiện Ví
    WalletResponse getUserWallet() throws WalletException;

    // nạp tiền vào ví
    WalletResponse addBalanceToWallet(Long amount) throws WalletException;

    // tìm ví
    WalletResponse findWalletById(Long id) throws WalletException;

    // Giữ lại phương thức trả về Entity nếu cần dùng nội bộ (optional)
    Wallet findWalletEntityById(Long id) throws WalletException;

    //    TODO Chuyển tiền từ ví người này sang ví người khác (OPTION)
//    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount) throws WalletException;

    //TODO có order rồi sẽ thanh toán cho việc mua bán tín chỉ carbon
//    public Wallet payOrderPayment(Order order, User user) throws WalletException;
}

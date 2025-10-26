package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // list theo danh sách order gần nhất
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    boolean existsByOrderAndTransactionType(Order order, WalletTransactionType transactionType);

    boolean existsByOrder(Order order);

    boolean existsByPaymentOrder(PaymentOrder paymentOrder);

    long count(); // Đếm tổng số giao dịch

    long countByWallet_Id(Long walletId); // Đếm số giao dịch của một ví cụ thể
    // SỬA TÊN PHƯƠNG THỨC Ở ĐÂY
    long countByWalletUserId(Long userId); // Đếm số giao dịch của một người dùng (thay vì countMyTransactions)
}

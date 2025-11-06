package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // list theo danh sách order gần nhất
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    long count(); // Đếm tổng số giao dịch

    long countByWalletUserId(Long userId); // Đếm số giao dịch của một người dùng (thay vì countMyTransactions)

    // Đếm số giao dịch mua cho một tín chỉ
    long countByOrderCarbonCreditIdAndTransactionType(
            Long creditId, WalletTransactionType transactionType);
    // Tìm giao dịch mua cho một tín chỉ
    List<WalletTransaction> findByOrderCarbonCreditIdAndTransactionType(
            Long creditId, WalletTransactionType transactionType);

}

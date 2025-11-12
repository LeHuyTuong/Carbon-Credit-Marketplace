package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.*;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // list theo danh sách order gần nhất
    @Query("SELECT wt FROM WalletTransaction wt LEFT JOIN FETCH wt.distribution WHERE wt.wallet = :wallet ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(@Param("wallet") Wallet wallet);
    long count(); // Đếm tổng số giao dịch

    long countByWalletUserId(Long userId); // Đếm số giao dịch của một người dùng (thay vì countMyTransactions)

    // Đếm số giao dịch mua cho một tín chỉ
    long countByOrderCarbonCreditIdAndTransactionType(
            Long creditId, WalletTransactionType transactionType);
    // Tìm giao dịch mua cho một tín chỉ
    List<WalletTransaction> findByOrderCarbonCreditIdAndTransactionType(
            Long creditId, WalletTransactionType transactionType);

}

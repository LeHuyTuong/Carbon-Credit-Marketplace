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
}

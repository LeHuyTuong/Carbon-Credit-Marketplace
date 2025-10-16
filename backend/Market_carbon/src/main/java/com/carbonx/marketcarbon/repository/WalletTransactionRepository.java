package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // list theo danh sách order gần nhất
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
}

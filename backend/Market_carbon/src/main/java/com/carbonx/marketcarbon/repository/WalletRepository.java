package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    WalletRepository finByUserId(Long userId);
}

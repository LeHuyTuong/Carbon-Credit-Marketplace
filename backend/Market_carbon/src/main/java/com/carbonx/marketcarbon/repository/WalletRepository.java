package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByUserId(Long userId);
    Optional<Wallet> findByCompany(Company company);

}

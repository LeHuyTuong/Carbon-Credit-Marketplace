package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerCompanyIdOrSellerCompanyId(Long buyerId, Long sellerId);
}

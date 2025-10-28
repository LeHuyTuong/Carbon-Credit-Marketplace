package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.Wallet;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByUserId(Long userId);
    Optional<Wallet> findByCompany(Company company);

    // Thêm phương thức tìm kiếm với khóa pessimistic
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Wallet findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}

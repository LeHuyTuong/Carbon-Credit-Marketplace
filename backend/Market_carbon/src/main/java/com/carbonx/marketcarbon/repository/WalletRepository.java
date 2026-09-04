package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByUserId(Long userId);

    // P1.3: id-only projection — lets callers take the row lock as the FIRST touch of the
    // entity (entityManager.find with PESSIMISTIC_WRITE), instead of preloading the wallet
    // unlocked and later "locking" a stale persistence-context copy (lost-update window).
    @Query("select w.id from Wallet w where w.company.id = :companyId")
    Long findIdByCompanyId(@Param("companyId") Long companyId);

    // Eager-load user and company in a single query to eliminate N+1
    // from @OneToOne EAGER default fetch on user and company
    @Query("SELECT w FROM Wallet w " +
           "LEFT JOIN FETCH w.user " +
           "LEFT JOIN FETCH w.company " +
           "WHERE w.user.id = :userId")
    Wallet findByUserIdWithDetails(@Param("userId") Long userId);

    // Eager-load user and company by company_id
    @Query("SELECT w FROM Wallet w " +
           "LEFT JOIN FETCH w.user " +
           "LEFT JOIN FETCH w.company " +
           "WHERE w.company.id = :companyId")
    Wallet findByCompanyIdWithDetails(@Param("companyId") Long companyId);

    Optional<Wallet> findByCompany(Company company);

    // Thêm phương thức tìm kiếm với khóa pessimistic
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Wallet findByUserIdWithPessimisticLock(@Param("userId") Long userId);

    Optional<Wallet> findByUser(User user);

}

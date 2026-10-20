package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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

    /**
     * Khoá row ví bằng SELECT ... FOR UPDATE (query, không phải entityManager.find).
     *
     * <p>Payout cần khoá ví của owner TRƯỚC khi kiểm tra "owner này đã được trả cho
     * report này chưa", để hai distribution khác nhau không trả tiền cho cùng một owner
     * song song. Query có @Lock luôn phát lệnh FOR UPDATE, kể cả khi entity đã được
     * load trước đó trong cùng persistence context.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    /**
     * Số dư TIỀN đã commit của ví, đọc dưới khoá row (SELECT ... FOR UPDATE).
     *
     * <p>Câu native này chỉ đọc MỘT cột nên không đụng vào phần state đang chờ của
     * entity: {@code entityManager.refresh} sẽ nạp lại toàn bộ state và xoá mất các
     * thay đổi chưa flush (ví dụ carbonCreditBalance mà luồng order vừa set trước khi
     * gọi createTransaction).</p>
     */
    @Query(value = "SELECT balance FROM wallets WHERE id = :id FOR UPDATE", nativeQuery = true)
    BigDecimal findBalanceByIdForUpdate(@Param("id") Long id);

    Optional<Wallet> findByUser(User user);

}

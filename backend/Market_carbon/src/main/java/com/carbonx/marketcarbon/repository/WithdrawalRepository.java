package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Withdrawal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRepository extends JpaRepository<Withdrawal,Long> {
    List<Withdrawal> findByUserId(Long id);

    /**
     * Danh sách đơn rút tiền KÈM tài khoản ngân hàng của người rút: admin phải thấy được
     * nơi nhận tiền, nếu không thì tiền đã bị trừ ví mà không ai chuyển được.
     */
    @Query("SELECT w FROM Withdrawal w LEFT JOIN FETCH w.paymentDetails WHERE w.user.id = :userId")
    List<Withdrawal> findByUserIdWithPaymentDetails(@Param("userId") Long userId);

    @Query("SELECT w FROM Withdrawal w LEFT JOIN FETCH w.paymentDetails")
    List<Withdrawal> findAllWithPaymentDetails();

    // P0-A (N3): lock the withdrawal row so concurrent process/reject attempts serialize
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Withdrawal w WHERE w.id = :id")
    Optional<Withdrawal> findByIdWithPessimisticLock(@Param("id") Long id);
}

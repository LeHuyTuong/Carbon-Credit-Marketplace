package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.PaymentOrder;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    PaymentOrder findPaymentById(long id);
    List<PaymentOrder> findPaymentByUserId(long userId);

//    Optional<PaymentOrder> findByVnpTxnRef(String vnpTxnRef);
//    // Add method with pessimistic lock for VNPay processing
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT po FROM PaymentOrder po WHERE po.vnpTxnRef = :vnpTxnRef")
//    Optional<PaymentOrder> findByVnpTxnRefWithLock(@Param("vnpTxnRef") String vnpTxnRef);

    // Add method with pessimistic lock for general processing by ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PaymentOrder po WHERE po.id = :id")
    Optional<PaymentOrder> findByIdWithLock(@Param("id") Long id);

}

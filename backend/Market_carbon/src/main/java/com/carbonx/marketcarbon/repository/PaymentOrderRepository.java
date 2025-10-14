package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    PaymentOrder findPaymentById(long id);
    List<PaymentOrder> findPaymentByUserId(long userId);
}

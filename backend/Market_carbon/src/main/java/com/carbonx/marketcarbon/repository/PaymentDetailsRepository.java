package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    PaymentDetails getPaymentDetailsByUserId(Long userId);
}

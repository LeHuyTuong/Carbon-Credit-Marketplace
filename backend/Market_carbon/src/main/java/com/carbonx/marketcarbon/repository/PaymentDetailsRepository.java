package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    Optional<PaymentDetails> findByUserIdAndAccountNumber(Long userId, String accountNumber);

    Optional<PaymentDetails> findFirstByUserIdOrderByIdDesc(Long userId);
}

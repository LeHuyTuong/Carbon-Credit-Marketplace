package com.example.Market_carbon.repository;

import com.example.Market_carbon.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByEmailAndUsedFalseOrderByIdDesc(String email);
}

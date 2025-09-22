package com.carbonx.marketcarbon.repository;



import com.carbonx.marketcarbon.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByEmailAndUsedFalseOrderByIdDesc(String email);
}

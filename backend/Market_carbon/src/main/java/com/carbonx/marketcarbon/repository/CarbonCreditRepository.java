package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, Long> {

    // Phương thức này dùng để tìm lô tín chỉ có sẵn của một công ty,
    Optional<CarbonCredit> findByCompanyAndStatus(Company owner, CreditStatus status);
    Page<CarbonCredit> findByStatus(CreditStatus status, Pageable pageable);
}

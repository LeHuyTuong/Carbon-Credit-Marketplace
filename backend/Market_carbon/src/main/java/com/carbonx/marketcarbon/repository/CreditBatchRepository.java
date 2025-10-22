package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.CreditBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditBatchRepository extends JpaRepository<CreditBatch, Long> {
    Optional<CreditBatch> findByReportId(Long reportId);
}

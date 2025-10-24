package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.CreditBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CreditBatchRepository extends JpaRepository<CreditBatch, Long>,
        JpaSpecificationExecutor<CreditBatch> {
    Optional<CreditBatch> findByReportId(Long reportId);
}

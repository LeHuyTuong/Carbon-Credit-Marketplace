package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.CreditBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CreditBatchRepository extends JpaRepository<CreditBatch, Long>,
        JpaSpecificationExecutor<CreditBatch> {
    Optional<CreditBatch> findByReportId(Long reportId);
    @EntityGraph(attributePaths = {"company","project","report","report.verifiedBy"})
    Optional<CreditBatch> findWithAllById(Long id);
    Optional<CreditBatch> findByCompanyId(Long projectId);

    /**
     * TÌM BATCH THEO BATCH CODE + COMPANY ID (BẢO MẬT)
     */
    Optional<CreditBatch> findByBatchCodeAndCompany_Id(String batchCode, Long companyId);
}

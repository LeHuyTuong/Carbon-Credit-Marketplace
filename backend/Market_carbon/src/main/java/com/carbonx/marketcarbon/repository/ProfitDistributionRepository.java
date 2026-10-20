package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfitDistributionRepository extends JpaRepository<ProfitDistribution, Long> {

    // Retry cần report + company trong cùng query vì hàm chạy async, ngoài transaction.
    @EntityGraph(attributePaths = {"emissionReport", "companyUser"})
    @Query("SELECT d FROM ProfitDistribution d WHERE d.id = :id")
    Optional<ProfitDistribution> findWithReportById(@Param("id") Long id);

    Optional<ProfitDistribution> findFirstByEmissionReportIdOrderByIdDesc(Long emissionReportId);

    // Compare-and-set: hai request retry đồng thời chỉ một request thắng.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.status = :retrying "
            + "WHERE d.id = :id AND d.status = :failed")
    int markRetrying(@Param("id") Long id,
                     @Param("retrying") ProfitDistributionStatus retrying,
                     @Param("failed") ProfitDistributionStatus failed);
}

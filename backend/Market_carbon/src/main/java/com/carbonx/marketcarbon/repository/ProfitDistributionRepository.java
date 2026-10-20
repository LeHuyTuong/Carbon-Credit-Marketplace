package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import com.carbonx.marketcarbon.model.ProfitDistribution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ProfitDistributionRepository extends JpaRepository<ProfitDistribution, Long> {

    // Retry cần report + company trong cùng query vì hàm chạy async, ngoài transaction.
    @EntityGraph(attributePaths = {"emissionReport", "companyUser", "project"})
    @Query("SELECT d FROM ProfitDistribution d WHERE d.id = :id")
    Optional<ProfitDistribution> findWithReportById(@Param("id") Long id);

    Optional<ProfitDistribution> findFirstByEmissionReportIdOrderByIdDesc(Long emissionReportId);

    // Dùng cho distribution cũ (trước khi có emission_report_id) để vẫn xác định
    // được đâu là lần chạy mới nhất của cùng công ty/dự án.
    Optional<ProfitDistribution> findFirstByCompanyUserIdAndProjectIdOrderByIdDesc(
            Long companyUserId, Long projectId);

    Optional<ProfitDistribution> findFirstByCompanyUserIdAndProjectIdAndEmissionReportIsNullOrderByIdDesc(
            Long companyUserId, Long projectId);

    Optional<ProfitDistribution> findFirstByCompanyUserIdOrderByIdDesc(Long companyUserId);

    // Compare-and-set: hai request retry đồng thời chỉ một request thắng.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.status = :retrying "
            + "WHERE d.id = :id AND d.status = :failed")
    int markRetrying(@Param("id") Long id,
                     @Param("retrying") ProfitDistributionStatus retrying,
                     @Param("failed") ProfitDistributionStatus failed);

    /**
     * Claim một distribution PROCESSING đã quá hạn heartbeat. Đây là đường thoát cho
     * trường hợp pod bị kill sau khi đã tạo distribution nhưng trước khi chạy xong payout.
     * Chỉ update khi status vẫn là PROCESSING và updatedAt còn cũ, nên hai request retry
     * đồng thời vẫn chỉ một request thắng.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :processing AND d.updatedAt < :staleBefore")
    int claimStaleProcessing(@Param("id") Long id,
                             @Param("processing") ProfitDistributionStatus processing,
                             @Param("staleBefore") OffsetDateTime staleBefore,
                             @Param("now") OffsetDateTime now);

    /**
     * Heartbeat sau mỗi owner: giữ distribution khỏi bị coi là stale trong khi job dài
     * vẫn đang chạy.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :processing")
    int touchProcessing(@Param("id") Long id,
                        @Param("processing") ProfitDistributionStatus processing,
                        @Param("now") OffsetDateTime now);
}

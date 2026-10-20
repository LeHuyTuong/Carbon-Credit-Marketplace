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

    /**
     * Compare-and-set: hai request retry đồng thời chỉ một request thắng.
     * Ghi kèm claim token và refresh updatedAt ngay tại thời điểm claim, nếu không
     * một request thứ hai đọc lại status PROCESSING sẽ thấy updatedAt cũ và claim
     * lại chính distribution vừa được claim (trả tiền hai lần).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.status = :retrying, d.claimToken = :claimToken, "
            + "d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :failed")
    int markRetrying(@Param("id") Long id,
                     @Param("retrying") ProfitDistributionStatus retrying,
                     @Param("failed") ProfitDistributionStatus failed,
                     @Param("claimToken") String claimToken,
                     @Param("now") OffsetDateTime now);

    /**
     * Claim một distribution PROCESSING đã quá hạn heartbeat. Đây là đường thoát cho
     * trường hợp pod bị kill sau khi đã tạo distribution nhưng trước khi chạy xong payout.
     * Chỉ update khi status vẫn là PROCESSING và updatedAt còn cũ; claim token đổi chủ
     * nên worker cũ không thể heartbeat/ghi trạng thái thay worker mới.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.claimToken = :claimToken, d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :processing AND d.updatedAt < :staleBefore")
    int claimStaleProcessing(@Param("id") Long id,
                             @Param("processing") ProfitDistributionStatus processing,
                             @Param("staleBefore") OffsetDateTime staleBefore,
                             @Param("now") OffsetDateTime now,
                             @Param("claimToken") String claimToken);

    /**
     * Heartbeat sau mỗi owner: giữ distribution khỏi bị coi là stale trong khi job dài
     * vẫn đang chạy. Chỉ worker còn giữ token mới heartbeat được.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :processing AND d.claimToken = :claimToken")
    int touchProcessing(@Param("id") Long id,
                        @Param("processing") ProfitDistributionStatus processing,
                        @Param("now") OffsetDateTime now,
                        @Param("claimToken") String claimToken);

    /**
     * Chuyển trạng thái cuối (COMPLETED/FAILED) chỉ khi worker còn giữ claim.
     * Trả 0 nghĩa là distribution đã bị worker khác claim; caller phải dừng và
     * không được ghi đè trạng thái của claim mới.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfitDistribution d SET d.status = :nextStatus, d.claimToken = NULL, "
            + "d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = :processing AND d.claimToken = :claimToken")
    int finishIfClaimed(@Param("id") Long id,
                        @Param("nextStatus") ProfitDistributionStatus nextStatus,
                        @Param("processing") ProfitDistributionStatus processing,
                        @Param("claimToken") String claimToken,
                        @Param("now") OffsetDateTime now);
}

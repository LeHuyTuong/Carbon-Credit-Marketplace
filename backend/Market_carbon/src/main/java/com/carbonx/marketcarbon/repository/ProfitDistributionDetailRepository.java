package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfitDistributionDetailRepository extends JpaRepository<ProfitDistributionDetail, Long> {

    //Tối ưu query khi load danh sách chi tiết phân phối lợi nhuận (ProfitDistributionDetail).
    //Load luôn thông tin chủ xe (EvOwner) và user của họ (User) trong cùng query.
    //Tránh tình trạng N+1 query khi bạn truy cập các quan hệ đó trong service hoặc DTO mapper.
    @EntityGraph(attributePaths = {"evOwner", "evOwner.user"})
    @Query("SELECT d FROM ProfitDistributionDetail d WHERE d.distribution.id = :distributionId")
    List<ProfitDistributionDetail> findByDistributionIdWithOwner(Long distributionId);

    boolean existsByDistributionIdAndStatusIgnoreCase(Long distributionId, String status);

    /**
     * Khóa row detail trong transaction trả tiền. Hai worker cùng đọc một detail
     * PENDING sẽ bị tuần tự hóa tại đây: worker thứ hai chờ worker đầu commit rồi
     * thấy SUCCESS và bỏ qua, thay vì cùng gọi transferFunds.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM ProfitDistributionDetail d WHERE d.id = :id")
    Optional<ProfitDistributionDetail> findByIdForUpdate(@Param("id") Long id);

    /**
     * Dùng cho legacy: distribution COMPLETED nhưng còn owner PENDING/FAILED vẫn
     * phải retry được. Ngược lại một distribution đã settled hết thì không retry.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.distribution.id = :distributionId "
            + "AND UPPER(d.status) NOT IN ('SUCCESS', 'SKIPPED')")
    boolean existsUnsettledByDistributionId(@Param("distributionId") Long distributionId);

    /**
     * Defense-in-depth cho dữ liệu cũ: nếu owner đã SUCCESS ở một distribution khác
     * cho cùng report thì không được trả tiền lần nữa.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.evOwner.id = :ownerId "
            + "AND d.status = 'SUCCESS' "
            + "AND d.distribution.emissionReport.id = :reportId "
            + "AND d.distribution.id <> :distributionId")
    boolean existsPaidInAnotherDistribution(@Param("reportId") Long reportId,
                                            @Param("ownerId") Long ownerId,
                                            @Param("distributionId") Long distributionId);

    /**
     * Cùng ý với {@link #existsPaidInAnotherDistribution} nhưng cho distribution legacy
     * (không có emission_report_id): đối chiếu theo company + project thay vì theo report.
     * Dùng để owner đã được trả ở một distribution khác của cùng dự án không bị trả
     * lần hai khi retry distribution legacy.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.evOwner.id = :ownerId "
            + "AND d.status = 'SUCCESS' "
            + "AND d.distribution.id <> :distributionId "
            + "AND d.distribution.companyUser.id = :companyUserId "
            + "AND d.distribution.project.id = :projectId")
    boolean existsPaidInAnotherDistributionOfProject(@Param("ownerId") Long ownerId,
                                                      @Param("companyUserId") Long companyUserId,
                                                      @Param("projectId") Long projectId,
                                                      @Param("distributionId") Long distributionId);

    /**
     * Bản fallback khi distribution legacy không còn thông tin project: đối chiếu theo
     * company để chặn trả tiền hai lần cho cùng một owner.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.evOwner.id = :ownerId "
            + "AND d.status = 'SUCCESS' "
            + "AND d.distribution.id <> :distributionId "
            + "AND d.distribution.companyUser.id = :companyUserId")
    boolean existsPaidInAnotherDistributionOfCompany(@Param("ownerId") Long ownerId,
                                                       @Param("companyUserId") Long companyUserId,
                                                       @Param("distributionId") Long distributionId);

    long countByDistributionId(Long distributionId);

    /**
     * Số owner CHƯA được trả của mỗi distribution (detail PENDING/FAILED) — để UI biết
     * đợt nào còn owner chưa nhận được tiền.
     */
    @Query("SELECT d.distribution.id, COUNT(d) FROM ProfitDistributionDetail d "
            + "WHERE d.distribution.id IN :distributionIds "
            + "AND UPPER(d.status) NOT IN ('SUCCESS', 'SKIPPED') "
            + "GROUP BY d.distribution.id")
    List<Object[]> countUnsettledByDistributionIds(@Param("distributionIds") List<Long> distributionIds);

    /**
     * Distribution legacy (không có emission_report_id) của cùng company + project được
     * tạo SAU khi report đã tồn tại là ứng viên có thể là payout của report đó.
     *
     * <p>Đây là cùng quy tắc mà migration backfill dùng: một distribution tạo sau report
     * thì có thể trả cho report đó, một distribution tạo trước report thì không thể (report
     * chưa tồn tại). Quy tắc này giữ /share cho các report MỚI mà vẫn chặn trả hai lần
     * cho report cũ.</p>
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.status = 'SUCCESS' "
            + "AND d.distribution.emissionReport IS NULL "
            + "AND d.distribution.companyUser.id = :companyUserId "
            + "AND d.distribution.project.id = :projectId "
            + "AND d.distribution.createAt >= :reportCreatedAt")
    boolean existsPaidLegacyCandidate(@Param("companyUserId") Long companyUserId,
                                       @Param("projectId") Long projectId,
                                       @Param("reportCreatedAt") OffsetDateTime reportCreatedAt);

    /**
     * Owner đã được trả bởi một distribution legacy ứng viên của report này.
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END "
            + "FROM ProfitDistributionDetail d "
            + "WHERE d.evOwner.id = :ownerId "
            + "AND d.status = 'SUCCESS' "
            + "AND d.distribution.id <> :distributionId "
            + "AND d.distribution.emissionReport IS NULL "
            + "AND d.distribution.companyUser.id = :companyUserId "
            + "AND d.distribution.project.id = :projectId "
            + "AND d.distribution.createAt >= :reportCreatedAt")
    boolean existsPaidInLegacyCandidate(@Param("ownerId") Long ownerId,
                                         @Param("companyUserId") Long companyUserId,
                                         @Param("projectId") Long projectId,
                                         @Param("distributionId") Long distributionId,
                                         @Param("reportCreatedAt") OffsetDateTime reportCreatedAt);
}

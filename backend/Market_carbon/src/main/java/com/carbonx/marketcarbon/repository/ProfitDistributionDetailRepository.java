package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}

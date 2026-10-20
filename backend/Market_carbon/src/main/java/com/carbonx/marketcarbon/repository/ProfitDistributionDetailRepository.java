package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.ProfitDistributionDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfitDistributionDetailRepository extends JpaRepository<ProfitDistributionDetail, Long> {

    //Tối ưu query khi load danh sách chi tiết phân phối lợi nhuận (ProfitDistributionDetail).
    //Load luôn thông tin chủ xe (EvOwner) và user của họ (User) trong cùng query.
    //Tránh tình trạng N+1 query khi bạn truy cập các quan hệ đó trong service hoặc DTO mapper.
    @EntityGraph(attributePaths = {"evOwner", "evOwner.user"})
    @Query("SELECT d FROM ProfitDistributionDetail d WHERE d.distribution.id = :distributionId")
    List<ProfitDistributionDetail> findByDistributionIdWithOwner(Long distributionId);

    boolean existsByDistributionIdAndStatusIgnoreCase(Long distributionId, String status);

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

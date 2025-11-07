package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.dashboard.MonthlyReportStatusDto;
import com.carbonx.marketcarbon.model.EmissionReport;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EmissionReportRepository extends JpaRepository<EmissionReport, Long>,
        JpaSpecificationExecutor<EmissionReport> {
    Optional<EmissionReport> findBySellerIdAndProjectIdAndPeriod(Long sellerId, Long projectId, String period);

    Page<EmissionReport> findBySourceIgnoreCase(String source, Pageable pageable);

    Page<EmissionReport> findBySourceIgnoreCaseAndStatus(String source, EmissionStatus status, Pageable pageable);

    Page<EmissionReport> findByStatus(EmissionStatus status, Pageable pageable);

    List<EmissionReport> findBySeller_Id(Long sellerId);

    List<EmissionReport> findBySeller_IdAndStatus(Long sellerId, EmissionStatus status);

    List<EmissionReport> findBySeller_IdAndProject_Id(Long sellerId, Long projectId);

    List<EmissionReport> findBySeller_IdAndProject_IdAndStatus(Long sellerId, Long projectId, EmissionStatus status);

    int countByProjectIdAndSeller_IdNot(Long projectId, Long sellerId);

    Optional<EmissionReport> findById(Long id);

    /**
     * Tìm tất cả các báo cáo theo trạng thái
     */
    List<EmissionReport> findByStatus(EmissionStatus status);


    /**
     * Tải EmissionReport và fetch EAGER collection 'details'
     * để tránh LazyInitializationException khi không có Transaction.
     */
    @Query("SELECT r FROM EmissionReport r WHERE r.id = :id")
    @EntityGraph(attributePaths = {"details"})
    Optional<EmissionReport> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(r) FROM EmissionReport r")
    long countAllReports();


    @Query(value = """
    SELECT 
        MONTHNAME(e.created_at) AS month,
        SUM(CASE WHEN e.status IN ('CVA_APPROVED', 'ADMIN_APPROVED', 'CREDIT_ISSUED', 'APPROVED', 'PAID_OUT') THEN 1 ELSE 0 END) AS approved,
        SUM(CASE WHEN e.status IN ('DRAFT', 'SUBMITTED') THEN 1 ELSE 0 END) AS pending,
        SUM(CASE WHEN e.status IN ('CVA_REJECTED', 'ADMIN_REJECTED', 'REJECTED') THEN 1 ELSE 0 END) AS rejected
    FROM emission_reports e
    GROUP BY MONTH(e.created_at), MONTHNAME(e.created_at)
    ORDER BY MONTH(e.created_at)
    """, nativeQuery = true)
    List<Object[]> countMonthlyReportStatusNative();


}

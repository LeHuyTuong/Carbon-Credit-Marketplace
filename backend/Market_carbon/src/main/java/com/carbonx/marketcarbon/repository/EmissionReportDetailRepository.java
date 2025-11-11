package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmissionReportDetailRepository extends JpaRepository<EmissionReportDetail, Long> {
    Page<EmissionReportDetail> findByReport_Id(Long reportId, Pageable pageable);
    List<EmissionReportDetail> findByReport_Id(Long reportId);

    // (tuỳ chọn) thêm tìm kiếm theo biển số
    Page<EmissionReportDetail> findByReport_IdAndVehiclePlateContainingIgnoreCase(
            Long reportId, String q, Pageable pageable);
    List<EmissionReportDetail> findByCompanyIdAndPeriod(Long companyId, String period);
    List<EmissionReportDetail> findByReport(EmissionReport report);
    @Query("""
SELECT COUNT(DISTINCT d2.report.id)
FROM EmissionReportDetail d1
JOIN EmissionReportDetail d2
  ON d1.vehiclePlate = d2.vehiclePlate
  AND d1.report.id <> d2.report.id
JOIN EmissionReport r2 ON r2.id = d2.report.id
WHERE d1.report.id = :reportId
  AND r2.seller.id <> :sellerId
""")
    int countDuplicatePlatesAcrossCompanies(Long reportId, Long sellerId);

}

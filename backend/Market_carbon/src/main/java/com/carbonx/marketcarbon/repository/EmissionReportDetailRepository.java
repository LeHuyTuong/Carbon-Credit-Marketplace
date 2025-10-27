package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmissionReportDetailRepository extends JpaRepository<EmissionReportDetail, Long> {
    Page<EmissionReportDetail> findByReport_Id(Long reportId, Pageable pageable);

    // (tuỳ chọn) thêm tìm kiếm theo biển số
    Page<EmissionReportDetail> findByReport_IdAndVehiclePlateContainingIgnoreCase(
            Long reportId, String q, Pageable pageable);
    List<EmissionReportDetail> findByReport(EmissionReport report);
}

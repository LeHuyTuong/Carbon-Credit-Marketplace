package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmissionReportDetailRepository extends JpaRepository<EmissionReportDetail, Long> {
    List<EmissionReportDetail> findByReportId(Long reportId);
    List<EmissionReportDetail> findByReport(EmissionReport report);
}

package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.model.EmissionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

}

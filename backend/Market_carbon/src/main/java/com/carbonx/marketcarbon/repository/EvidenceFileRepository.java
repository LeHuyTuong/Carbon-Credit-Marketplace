package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EvidenceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, Long> {
    Page<EvidenceFile> findByReportId(Long reportId, Pageable pageable);
}

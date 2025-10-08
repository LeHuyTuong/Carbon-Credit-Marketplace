package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EmissionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmissionReportRepository extends JpaRepository<EmissionReport, Long>,
        JpaSpecificationExecutor<EmissionReport> {}

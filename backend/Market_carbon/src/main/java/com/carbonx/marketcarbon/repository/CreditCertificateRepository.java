package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.CreditBatch;
import com.carbonx.marketcarbon.model.CreditCertificate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditCertificateRepository extends JpaRepository<CreditCertificate, Long> {

    Optional<CreditCertificate> findByCertificateCode(String certificateCode);

    Optional<CreditCertificate> findByBatch_Id(Long batchId);

    boolean existsByBatch_Id(Long batchId);

    // Dùng lại tên findById và thêm EntityGraph để load đủ quan hệ
    @EntityGraph(attributePaths = {
            "batch",
            "batch.company",
            "batch.project",
            "batch.report",
            "batch.report.verifiedBy"
    })
    Optional<CreditCertificate> findById(Long id);

    //  tìm theo code nhưng fetch đầy đủ
    @EntityGraph(attributePaths = {
            "batch",
            "batch.company",
            "batch.project",
            "batch.report",
            "batch.report.verifiedBy"
    })
    Optional<CreditCertificate> findWithAllByCertificateCode(String certificateCode);

    // repository/CreditCertificateRepository.java
    Optional<CreditCertificate> findTopByBatchOrderByIdDesc(CreditBatch batch);

}

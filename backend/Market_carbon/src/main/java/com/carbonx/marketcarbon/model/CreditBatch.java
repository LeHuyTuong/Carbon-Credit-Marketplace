package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "credit_batches",
        uniqueConstraints = @UniqueConstraint(name = "uk_batch_report", columnNames = "report_id"),
        indexes = @Index(name = "idx_batch_code", columnList = "batch_code", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditBatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private EmissionReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "batch_code", length = 64, nullable = false, unique = true)
    private String batchCode;               // ví dụ: 2025-VFV12-XED3-000001_000050

    @Column(precision = 18, scale = 3, nullable = false)
    private BigDecimal totalTco2e;          // tổng tCO2e sau điều chỉnh

    @Column(name = "credits_count", nullable = false)
    private Integer creditsCount;           // số tín chỉ (floor)

    @Column(precision = 18, scale = 3, nullable = false)
    private BigDecimal residualTco2e;       // phần dư

    @Column(name = "vintage_year", nullable = false)
    private Integer vintageYear;

    @Column(name = "serial_prefix", length = 64, nullable = false)
    private String serialPrefix;            // 2025-VFV12-XED3-

    @Column(name = "serial_from", nullable = false)
    private Long serialFrom;

    @Column(name = "serial_to", nullable = false)
    private Long serialTo;

    @Column(length = 16, nullable = false)
    private String status;                  // ISSUED / CERTIFIED / ...

    private OffsetDateTime issuedAt;
    private OffsetDateTime createdAt;

    @Column(name = "issued_by", length = 100)
    private String issuedBy;

    @PrePersist
    void pre() {
        createdAt = OffsetDateTime.now();
        if (issuedAt == null) issuedAt = createdAt;
        if (status == null) status = "ISSUED";
    }
}


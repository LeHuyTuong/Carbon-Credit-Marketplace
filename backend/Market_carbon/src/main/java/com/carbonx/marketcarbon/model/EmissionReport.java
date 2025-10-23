// src/main/java/com/carbonx/marketcarbon/model/EmissionReport.java
package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "emission_reports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_seller_project_period", columnNames = {"seller_id","project_id","period"})
        }
)
public class EmissionReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    Company seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    Project project;

    @Column(length = 16, nullable = false)
    String period; // ví dụ "2025-09" hoặc "2025-Q3"

    @Column(name = "total_energy", precision = 14, scale = 4, nullable = false)
    @Builder.Default
    BigDecimal totalEnergy = BigDecimal.ZERO;

    @Column(name = "total_co2", precision = 14, scale = 4, nullable = false)
    @Builder.Default
    BigDecimal totalCo2 = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Builder.Default
    EmissionStatus status = EmissionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    User verifiedBy;

    @Column(columnDefinition = "text")
    String comment;

    // Nguồn tạo report: dùng String, mặc định "CSV"
    @Column(name = "source", length = 16, nullable = false)
    String source;

    // Tổng số xe (Company tự nhập từ CSV)
    @Column(name = "vehicle_count", nullable = false)
    @Builder.Default
    Integer vehicleCount = 0;

    // Metadata file CSV upload
    String uploadOriginalFilename;
    String uploadMimeType;
    Long   uploadSizeBytes;
    @Column(length = 128) String uploadSha256;
    String uploadStorageKey;
    @Column(length = 1024) String uploadStorageUrl; // URL dài -> tăng length
    Integer uploadRows;
    @Column(columnDefinition = "text") String parseError;

    // Mốc thời gian
    OffsetDateTime createdAt;
    OffsetDateTime submittedAt;
    OffsetDateTime verifiedAt;
    OffsetDateTime approvedAt;
    OffsetDateTime updatedAt;

    @Column(name = "ai_pre_score", precision = 4, scale = 2)
    BigDecimal aiPreScore;

    @Column(name = "ai_version", length = 16)
    String aiVersion;

    @Column(name = "ai_pre_notes", columnDefinition = "text")
    String aiPreNotes;

    @Column(name = "verification_score", precision = 4, scale = 2)
    BigDecimal verificationScore;

    @Column(name = "verification_comment", columnDefinition = "text")
    String verificationComment;

    @PrePersist
    void prePersist() {
        if (source == null || source.isBlank()) source = "CSV";
        source = source.toUpperCase();
    }

    @PreUpdate
    void preUpdate() {
        if (source != null) source = source.toUpperCase();
    }
}

// src/main/java/com/carbonx/marketcarbon/model/EmissionReport.java
package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "residual_tco2e", precision = 10, scale = 3)
    @Builder.Default
    BigDecimal residualTco2e = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Builder.Default
    EmissionStatus status = EmissionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_cva_id")
    Cva verifiedByCva;

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
    LocalDateTime createdAt;
    LocalDateTime submittedAt;
    LocalDateTime verifiedAt;
    LocalDateTime approvedAt;
    LocalDateTime updatedAt;

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

    @Column(name = "verified_by_cva_name", length = 128)
    String verifiedByCvaName;

    @Column(name = "admin_approved_by_name", length = 128)
    String adminApprovedByName;

    @PrePersist
    void prePersist() {
        if (source == null || source.isBlank()) source = "CSV";
        source = source.toUpperCase();
    }

    @PreUpdate
    void preUpdate() {
        if (source != null) source = source.toUpperCase();
    }

    /**
     * Quan hệ Một-Nhiều (One-to-Many) tới các dòng chi tiết.
     * Đây là "inverse side" (phía không sở hữu) của quan hệ.
     */
    @OneToMany(
            mappedBy = "report", // "report" là tên trường @ManyToOne trong EmissionReportDetail
            cascade = CascadeType.ALL, // Tự động lưu/xóa 'details' khi 'report' được lưu/xóa
            orphanRemoval = true, // Tự động xóa 'details' khỏi DB nếu chúng bị gỡ khỏi list này
            fetch = FetchType.LAZY // Chỉ load danh sách này khi gọi .getDetails()
    )
    @JsonIgnore // Ngăn lỗi lặp vô hạn (infinite loop) khi serialize JSON
    @Builder.Default
    private List<EmissionReportDetail> details = new ArrayList<>();

}

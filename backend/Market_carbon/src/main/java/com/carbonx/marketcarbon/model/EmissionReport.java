package com.carbonx.marketcarbon.model;


// === model/EmissionReport.java (thay cho PeriodicReport)

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class EmissionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Công ty nộp báo cáo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    Company seller;

    // Dự án (ví dụ: VF)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    Project project;

    // Nếu bạn thực sự cần gắn report với 1 vehicle thì để optional; còn tổng hợp theo company thì nên bỏ hẳn field này
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    Vehicle vehicle;

    // Kỳ báo cáo "YYYY-MM" hoặc "YYYY-Qn"
    @Column(length = 16, nullable = false)
    String period;

    // Tổng điện năng sạc trong kỳ (kWh)
    @Column(name = "total_energy", precision = 14, scale = 4, nullable = false)
    @Builder.Default
    BigDecimal totalEnergy = BigDecimal.ZERO;

    // Tổng CO2 quy đổi (kg)
    @Column(name = "total_co2", precision = 14, scale = 4, nullable = false)
    @Builder.Default
    BigDecimal totalCo2 = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Builder.Default
    EmissionStatus status = EmissionStatus.DRAFT;

    // Người xác minh (verifier) – dùng User cho linh hoạt (CVA/Admin đều là User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    User verifiedBy;

    @Column(name = "comment", columnDefinition = "text")
    String comment;

    // Mốc thời gian
    OffsetDateTime createdAt;
    OffsetDateTime submittedAt;
    OffsetDateTime verifiedAt;
    OffsetDateTime approvedAt;
    OffsetDateTime updatedAt;

    // Chứng từ kèm theo
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    java.util.List<EvidenceFile> evidences = new java.util.ArrayList<>();
}

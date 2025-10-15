package com.carbonx.marketcarbon.model;


// === model/EmissionReport.java (thay cho PeriodicReport)

import com.carbonx.marketcarbon.common.EmissionStatus;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id; // report_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Company seller;      // công ty nộp

    @ManyToOne(optional = false)
    Vehicle vehicle;             // xe liên quan

    String period;

    @Column(precision = 12, scale = 4)
    BigDecimal calculatedCo2;

    @Column(precision = 12, scale = 4)
    BigDecimal baselineIceCo2;

    @Column(precision = 12, scale = 4)
    BigDecimal evCo2;

    @Enumerated(EnumType.STRING)
    EmissionStatus status;

    // người CVA đã review/approve bước 1
    @ManyToOne @JoinColumn(name="reviewed_by")
    private Cva reviewedBy;

    // người Admin phê duyệt cuối
    @ManyToOne @JoinColumn(name="approved_by")
    private Admin approvedBy;

    // liên kết tới tín chỉ (nếu bạn đã có entity CarbonCredit)
    @ManyToOne
    @JoinColumn(name = "credit_id")
    Co2Credit credit;

    @Column(columnDefinition = "text")
    String comment;

    OffsetDateTime createdAt;
    OffsetDateTime submittedAt;
    OffsetDateTime approvedAt;
    OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    List<EvidenceFile> evidences = new ArrayList<>();
}

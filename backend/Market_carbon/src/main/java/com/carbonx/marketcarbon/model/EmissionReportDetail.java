package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "emission_report_details")
public class EmissionReportDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Cha tổng hợp
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    EmissionReport report;

    // Các thông tin dòng CSV (để audit)
    @Column(length = 16, nullable = false)
    String period;          // ví dụ "2025-10"

    @Column(name = "company_id", nullable = false)
    Long companyId;

    @Column(name = "project_id", nullable = false)
    Long projectId;


    @Column(name = "total_energy", precision = 18, scale = 4, nullable = false)
    BigDecimal totalEnergy;

    @Column(name = "co2_kg", precision = 18, scale = 4, nullable = false)
    BigDecimal co2Kg;

    @Column(name = "vehicle_plate", length = 50)
    private String vehiclePlate;
}

package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 16, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(columnDefinition = "TEXT")
    private String commitments;

    @Column(columnDefinition = "TEXT")
    private String technicalIndicators;

    @Column(columnDefinition = "TEXT")
    private String measurementMethod;

    @Column(length = 255)
    private String legalDocsFile;

    @Column(length = 512)
    private String logo;

    @Column(length = 512)
    private String getEmissionFactorKgPerKwh;


    @Column(name = "emission_factor_kg_per_kwh", precision = 10, scale = 4)
    BigDecimal emissionFactorKgPerKwh;


    @Column(name = "buffer_reserve_pct", precision = 5, scale = 4)
    BigDecimal bufferReservePct;


    @Column(name = "uncertainty_pct", precision = 5, scale = 4)
    BigDecimal uncertaintyPct;


    @Column(name = "leakage_pct", precision = 5, scale = 4)
    BigDecimal leakagePct;

    @Column(name = "start_date")
    private LocalDate startedDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectApplication> applications = new ArrayList<>();
}
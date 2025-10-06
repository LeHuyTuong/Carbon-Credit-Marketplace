package com.carbonx.marketcarbon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name="vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number",length=64, unique = true, nullable=false)
    private String plateNumber;

    @Column(length=128, nullable=false)
    private String brand;

    @Column(length=128,  nullable=false)
    private String model;

    @Column( nullable=false)
    @Min(1950) @Max(2025)
    private Integer yearOfManufacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = true)
    @JsonIgnore // ẳn quan hệ khỏi Json tránh vòng lặp tuần hoàn
    private Company company;
}

package com.carbonx.marketcarbon.model;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable =false)
    private User owner;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="company_id", nullable=true)
    private Company company;

}

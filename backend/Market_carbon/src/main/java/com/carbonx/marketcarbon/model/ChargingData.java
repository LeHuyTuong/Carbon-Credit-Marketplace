package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "charging_data")
@Builder
public class ChargingData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charging_data_id")
    private Long id;

    @NotNull
    @DecimalMin("0.000")
    @Column(name = "charging_energy", precision = 12, scale = 3, nullable = false)
    private BigDecimal chargingEnergy;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;           // BẮT BUỘC – thay cho plateNumber

}
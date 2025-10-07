package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
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
    private String plateNumber;
    private BigDecimal chargingEnergy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long companyId;
}

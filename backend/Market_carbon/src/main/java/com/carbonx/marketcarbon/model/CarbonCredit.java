package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carbon_credits")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CarbonCredit extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal carbonCredit;
    private Long companyId;

    @Enumerated(EnumType.STRING)
    private CreditStatus status;

    private LocalDateTime issueAt;

    @JsonProperty("name")
    private String name;

    @JsonProperty("current_price")
    private double currentPrice;

    @ManyToOne
    @JoinColumn(name = "charging_data_id")
    private ChargingData chargingData;
}

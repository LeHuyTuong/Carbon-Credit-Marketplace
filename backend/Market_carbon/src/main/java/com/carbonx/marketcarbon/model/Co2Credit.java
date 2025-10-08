package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "co2_credit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Co2Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String creditCode;
    private Double amount;
    private String type;

    @ManyToOne
    @JoinColumn(name = "emission_report_id")
    private EmissionReport emissionReport;
}

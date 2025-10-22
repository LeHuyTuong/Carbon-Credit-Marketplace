package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_serial_counters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_serial_counter",
                columnNames = {"vintage_year", "project_id", "company_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditSerialCounter {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vintage_year", nullable = false)
    private Integer vintageYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "next_serial", nullable = false)
    private Long nextSerial;
}
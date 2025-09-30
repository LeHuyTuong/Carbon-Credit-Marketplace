package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.domain.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="carbon_certificates", indexes = {
        @Index(name="idx_certificate_user", columnList = "userId",  unique = true),
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private Long owner_id;

    @Column(nullable=false)
    private Long verification_id;

    @Column(nullable=false, length=100)
    private String serial_no;

    @Column(nullable=false)
    private int vintage_year;

    @Column(nullable=false)
    private double total_tco2e;
    @Column(nullable=false)
    private double available_tco2e;

    private Status status;

    private LocalDate issued_at;

    private LocalDate retired_at ;
}

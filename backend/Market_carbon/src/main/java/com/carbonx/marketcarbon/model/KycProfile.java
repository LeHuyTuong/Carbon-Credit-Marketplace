package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.domain.IDType;
import com.carbonx.marketcarbon.domain.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name="kyc_profiles", indexes = {
        @Index(name="idx_kyc_user", columnList = "userId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false)
    private String name;

    @Column(length=32)
    private String phone;

    @Column(length=100)
    private String country;

    @Column(length=100)
    private String address;

    private LocalDate birthDate;

    @Column(length=100, unique=true, nullable=false)
    private String email;

    @Column(nullable=false, length=16)
    private KycStatus kycStatus = KycStatus.NEW; // NONE,PENDING,VERIFIED,REJECTED

    @Column(length=32)
    private IDType documentType;

    @Column(length=64)
    private String documentNumber;

    private Instant updatedAt;
}

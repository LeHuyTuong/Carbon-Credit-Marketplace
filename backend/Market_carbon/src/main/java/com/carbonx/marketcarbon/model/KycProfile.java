package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    @Column(length=32)
    private String phone;

    @Column(length=100)
    private String country;

    @Column(nullable=false, length=16)
    private String kycStatus; // NONE,PENDING,VERIFIED,REJECTED

    @Column(length=32)
    private String documentType;

    @Column(length=64)
    private String documentNumber;

    @Column(length=512)
    private String documentUrl;

    private Instant updatedAt;
}

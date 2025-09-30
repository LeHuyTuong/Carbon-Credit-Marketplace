package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true)
    private Long userId;

    @Column(nullable=false)
    private String name;

    @Column(length=32, nullable=false)
    private String phone;

    @Column(length=100 , nullable=false)
    private String country;

    @Column(length=100, nullable = false)
    private String address;

    @Column(nullable=false)
    private LocalDate birthDate;

    @Column(length=100, unique=true, nullable=false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private KycStatus kycStatus = KycStatus.NEW; // NONE,PENDING,VERIFIED,REJECTED

    @Column(length=32)
    @Enumerated(EnumType.STRING)
    private IDType documentType;

    @Column(length=64, nullable = false, unique=true)
    private String documentNumber;


}

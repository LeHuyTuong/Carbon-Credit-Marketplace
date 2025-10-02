package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_ROLE;
import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 255)
    private String email;

    @Column(nullable=false, length = 255)
    private String passwordHash;

    @Column(nullable=false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 16)
    @Builder.Default
    private USER_STATUS status = USER_STATUS.PENDING;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expired_at")
    private OffsetDateTime otpExpiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 50)
    private USER_ROLE role;

    @ManyToOne(fetch =  FetchType.EAGER)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

}


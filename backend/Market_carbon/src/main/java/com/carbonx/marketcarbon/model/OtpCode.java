package com.carbonx.marketcarbon.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="otp_codes", indexes = {
        @Index(name="idx_otp_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Column(nullable=false, length=10)
    private String code;

    @Column(nullable=false)
    private Instant expiresAt;

    @Column(nullable=false)
    private boolean used;

    @Column(nullable=false, length=255)
    private String toEmail;

    @Column(nullable=false, length=50)
    private String subject;

    @Column(nullable=false, length=255)
    private String messageBody;
}

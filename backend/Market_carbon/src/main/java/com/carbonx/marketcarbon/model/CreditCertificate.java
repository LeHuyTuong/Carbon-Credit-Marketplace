package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "credit_certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_certificate_code", columnNames = "certificate_code"),
                @UniqueConstraint(name = "uk_certificate_batch", columnNames = "batch_id")
        },
        indexes = {
                @Index(name = "idx_certificate_code", columnList = "certificate_code")
        })
public class CreditCertificate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi batch chỉ có 1 certificate
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CreditBatch batch;

    @Column(name = "certificate_code", length = 128, nullable = false, unique = true)
    private String certificateCode;

    // nơi bạn upload PDF (S3/local) – lưu link và key
    @Column(name = "pdf_url", length = 2048)
    private String pdfUrl;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    // Ai được cấp (hiển thị trên chứng chỉ)
    @Column(name = "issued_to", length = 256)
    private String issuedTo;

    @Column(name = "issued_email", length = 256)
    private String issuedEmail;

    @Column(name = "verify_url", length = 1024)
    private String verifyUrl;

    @Column(name = "qr_code_url", length = 1024)
    private String qrCodeUrl;

    @Column(name = "registry", length = 256)
    private String registry; // ví dụ: CarbonX Internal Registry

    @Column(name = "standard", length = 256)
    private String standard; // ví dụ: ISO 14064-2 aligned

    @Column(name = "methodology", length = 256)
    private String methodology; // ví dụ: EV Charging Methodology v1.0

    private LocalDateTime issuedAt;

    @Column(name = "certificate_url", length = 1024)
    private String certificateUrl;

    @PrePersist
    void pre() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}

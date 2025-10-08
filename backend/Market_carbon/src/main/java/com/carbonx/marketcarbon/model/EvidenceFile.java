package com.carbonx.marketcarbon.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evidence_files",
        indexes = {
                @Index(name = "idx_evidence_report", columnList = "report_id"),
                @Index(name = "idx_evidence_storage_key", columnList = "storage_key")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EvidenceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private EmissionReport report;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    /** URL public (S3/CloudFront/MinIO) để FE truy cập */
    @Column(name = "storage_url", length = 1024)
    private String storageUrl;

    /** Object key trên S3 để quản trị (xóa/di chuyển/CDN). */
    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    /** Trạng thái kiểm tra bởi CVA */
    @Builder.Default
    @Column(name = "checked_by_cva", nullable = false)
    private Boolean checkedByCva = Boolean.FALSE;

    @Column(name = "cva_note", length = 1000)
    private String cvaNote;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) uploadedAt = OffsetDateTime.now();
        if (checkedByCva == null) checkedByCva = Boolean.FALSE;
    }
}
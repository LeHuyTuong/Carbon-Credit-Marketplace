package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "project_application",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_project_company",
                        columnNames = {"project_id", "company_id"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProjectApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Trạng thái luồng xử lý của đơn
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    // CVA reviewer và Admin reviewer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Cva reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_reviewer_id")
    private Admin finalReviewer;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @Column(columnDefinition = "TEXT")
    private String finalReviewNote;

    @Column(name = "application_docs_path", length = 512)
    private String applicationDocsPath;

    @Column(name = "application_docs_url", length = 512)
    private String applicationDocsUrl;

    @Column(name = "submitted_at")
    private java.time.OffsetDateTime submittedAt;

}

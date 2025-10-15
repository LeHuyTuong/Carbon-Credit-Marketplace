package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Entity
@Table(name = "project")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false,  length = 100)
    private String title;

    @Column(nullable = false,  length = 255)
    private String description;

    @URL
    @Column(nullable = false,  length = 255)
    private String logo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,  length = 10)
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "parent_project_id")
    private Long parentProjectId;

    @Column(columnDefinition = "TEXT")
    private String commitments;            // Cam kết giảm phát thải

    @Column(columnDefinition = "TEXT")
    private String technicalIndicators;    // Các chỉ số kỹ thuật

    @Column(columnDefinition = "TEXT")
    private String measurementMethod;      // Phương pháp đo lường

    @Column(length = 255)
    private String legalDocsUrl;           // Link tài liệu pháp lý (S3/…)

    // ==== Thông tin thẩm định của CVA/đơn vị thẩm định ====
    @Column(length = 20)
    private String reviewer;               // Tài khoản/tên đơn vị thẩm định

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin finalReviewer;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;             // Nhận xét khi duyệt
}

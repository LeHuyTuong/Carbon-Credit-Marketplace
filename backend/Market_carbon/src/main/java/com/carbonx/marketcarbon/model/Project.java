package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                         // ID duy nhất của dự án

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;            // DRAFT, ACTIVE, CLOSED, ...

    @Column(columnDefinition = "TEXT")
    private String commitments;

    @Column(columnDefinition = "TEXT")
    private String technicalIndicators;

    @Column(columnDefinition = "TEXT")
    private String measurementMethod;

    @Column(length = 255)
    private String legalDocsUrl;

    // Liên kết ngược: Một Project có thể có nhiều đơn đăng ký
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectApplication> applications = new ArrayList<>();
}
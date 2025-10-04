package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.Status;
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

    @Column(unique = true, nullable = false,  length = 20)
    private String title;

    @Column(nullable = false,  length = 255)
    private String description;

    @URL
    @Column(nullable = false,  length = 255)
    private String logo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,  length = 10)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

}

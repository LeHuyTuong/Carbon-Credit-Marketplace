package com.carbonx.marketcarbon.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
@Builder
public class Company extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(name = "business_license", nullable = false, length = 100)
    private String businessLicense;

    @Column(name = "tax_code", length = 100)
    private String taxCode;

    @Column(name = "company_name", length = 15)
    private String companyName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @OneToMany(mappedBy = "company")
    private List<EVOwner> evOwners = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarbonCredit> carbonCredits = new ArrayList<>();

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Wallet wallet;

    @OneToMany(
            mappedBy = "company",
            fetch = FetchType.LAZY,
            cascade = { CascadeType.MERGE}
    )
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectApplication> applications = new ArrayList<>();
}

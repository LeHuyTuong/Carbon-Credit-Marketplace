package com.carbonx.marketcarbon.model;


import com.carbonx.marketcarbon.common.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "company")
public class Company extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=20, unique=true)
    private String name;

    @Column(nullable=false, length=50)
    private String address;

    @Column(nullable=false, length=50, unique=true)
    private String tax_code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @OneToMany(
            mappedBy = "company",
            fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            orphanRemoval = false
    )
    @Column(nullable=true)
    private List<User> users = new ArrayList<>();

    @OneToMany(
            mappedBy = "company",
            fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            orphanRemoval = false
    )
    @Column(nullable=true)
    private List<Vehicle> vehicles = new ArrayList<>();
}

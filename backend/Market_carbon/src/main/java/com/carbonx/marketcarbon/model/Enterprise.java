package com.carbonx.marketcarbon.model;


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
@Table(name = "enterprise")
public class Enterprise extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=20, unique=true)
    private String name;

    @Column(nullable=false, length=50)
    private String address;

    @Column(nullable=false, length=50, unique=true)
    private String tax_code;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL) // 1 Enterprise gồm nhiều user
    @JoinColumn(name = "enterprise_id") // đặt FK ở bảng users
    private List<User> users = new ArrayList<>();

}

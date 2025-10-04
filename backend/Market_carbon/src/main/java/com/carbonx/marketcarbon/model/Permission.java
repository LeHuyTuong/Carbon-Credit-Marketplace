package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Getter @Setter
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permId;

    private String code;
    private String description;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}


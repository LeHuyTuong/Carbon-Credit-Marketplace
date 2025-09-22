package com.example.Market_carbon.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 255)
    private String email;

    @Column(nullable=false, length = 255)
    private String passwordHash;

    @Column(nullable=false, length = 255)
    private String fullName;

    @Column(nullable=false, length = 32)
    private String role; // EV_OWNER, CC_BUYER, CVA, ADMIN

    @Column(nullable=false, length = 16)
    private String status; // ACTIVE, SUSPENDED
}


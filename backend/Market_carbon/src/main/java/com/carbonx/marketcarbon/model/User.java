package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_STATUS;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 255)
    private String email;

    @Column(nullable=false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 16)
    @Builder.Default
    private USER_STATUS status = USER_STATUS.PENDING;

    @Column(name = "otp_code")
    @JsonIgnore
    private String otpCode;

    @Column(name = "otp_expiry_at")
    @JsonIgnore
    LocalDateTime otpExpiryDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToOne
    @JsonBackReference
    private Wallet wallet;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<PaymentOrder> paymentOrders = new ArrayList<>();
}


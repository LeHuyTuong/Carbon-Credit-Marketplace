package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_ROLE;
import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    private String passwordHash;

    @Column(nullable=false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 16)
    @Builder.Default
    private USER_STATUS status = USER_STATUS.PENDING;

    @Column(name = "otp_code")
    private String otpCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = true) // cho phép null nếu register chưa có DN
    @JoinColumn(name = "company_id", nullable = true) // cột FK cho phép NULL, không default 0
    private Company company;

    @Column(name = "otp_expiry_at")
    LocalDateTime otpExpiryDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

}


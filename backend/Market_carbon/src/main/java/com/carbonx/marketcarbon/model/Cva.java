package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity @Table(name="cva",
        indexes = {@Index(name="ix_cva_status", columnList="status")}
)
public class Cva {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="cva_id")
    Long id;

    @OneToOne(optional = false) @JoinColumn(name="user_id", unique = true)
    User user;

    @Column(unique = true) String code;
    @Column(nullable = false) String name;
    @Column(nullable = false) String email;
    String organization;
    @Column(name="position_title") String positionTitle;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    USER_STATUS status;

    String avatarUrl;

    OffsetDateTime createdAt; OffsetDateTime updatedAt;
    @PrePersist void preP(){
        createdAt = updatedAt = OffsetDateTime.now(); if (status==null) status=USER_STATUS.ACTIVE; }
    @PreUpdate  void preU(){
        updatedAt = OffsetDateTime.now(); }

    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (organization != null && !organization.isBlank()) {
            return organization;
        }
        return user != null ? user.getEmail() : "Unknown CVA";
    }
}
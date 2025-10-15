package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity @Table(name="admin",
        indexes = {@Index(name="ix_admin_status", columnList="status"), @Index(name="ix_admin_last_active", columnList="last_active_at DESC")}
)
public class Admin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="admin_id")
    Long id;

    @OneToOne(optional = false) @JoinColumn(name="user_id", unique = true)
    User user;

    @Column(unique = true) String code;
    @Column(nullable = false) String name;
    String phone;

    @Column(name="is_super_admin", nullable = false) Boolean isSuperAdmin;

    @Column(name="position_title",nullable = false) String positionTitle;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    USER_STATUS status;

    OffsetDateTime createdAt; OffsetDateTime updatedAt;
    @PrePersist void preP(){
        createdAt = updatedAt = OffsetDateTime.now(); if (status==null) status=USER_STATUS.ACTIVE; if (isSuperAdmin==null) isSuperAdmin=false; }
    @PreUpdate  void preU(){
        updatedAt = OffsetDateTime.now(); }
}
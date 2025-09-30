package com.carbonx.marketcarbon.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity {
    @Column(name = "create_at" , nullable = false, updatable = false)
    OffsetDateTime createAt;

    @Column(name = "updated_at" , nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

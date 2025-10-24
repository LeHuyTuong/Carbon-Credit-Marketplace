package com.carbonx.marketcarbon.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity {
    @CreationTimestamp  // Hibernate sẽ auto set timestamp khi persist/update, không cần @PrePersist.
    @Column(name = "created_at" , nullable = false, updatable = false)
    OffsetDateTime createAt;

    @UpdateTimestamp
    @Column(name = "updated_at" , nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); // set giờ việt nam UTC + 7
        createAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}

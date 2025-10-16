package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.USER_STATUS;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity @Table(name="admin")

public class Admin extends BaseEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="admin_id")
    Long id;

    @OneToOne(optional = false) @JoinColumn(name="user_id", unique = true)
    User user;

    @Column(nullable = false) String name;
    String phone;

}
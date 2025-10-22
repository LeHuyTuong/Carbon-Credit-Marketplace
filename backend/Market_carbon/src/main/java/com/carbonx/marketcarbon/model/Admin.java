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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    User user;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 15)
    String phone;

    @Column(length = 50)
    String firstName;

    @Column(length = 50)
    String lastName;

    @Column(length = 100)
    String country;

    @Column(length = 100)
    String city;

    @Column(length = 15)
    String birthday;

    @Column(length = 512)
    String avatarUrl;

}
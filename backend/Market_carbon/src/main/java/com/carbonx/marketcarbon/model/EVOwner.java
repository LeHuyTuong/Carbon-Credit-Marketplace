package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.annotation.DocumentNumber;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Table(name="ev_owner", indexes = {
        @Index(name="idx_ev_owner", columnList = "user_id", unique = true)
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EVOwner extends BaseEntity{

    @Id
    @Column(name = "user_id")       // PK = users.id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId                         // chia sẻ PK với user.id
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false)
    private String name;

    @Column(length=32, nullable=false)
    private String phone;

    @Column(length=100 , nullable=false)
    private String country;

    @Column(length=100, nullable = false)
    private String address;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(length=100, unique=true, nullable=false)
    private String email;

    @Column(length=64, nullable=false)
    @Enumerated(EnumType.STRING)
    private IDType documentType;

    @DocumentNumber(message = "document Number invalid format")
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // nullable: EV Owner có thể chưa thuộc công ty
    private Company company;

}

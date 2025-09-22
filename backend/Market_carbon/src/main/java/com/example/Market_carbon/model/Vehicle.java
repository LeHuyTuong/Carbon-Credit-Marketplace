package com.example.Market_carbon.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long ownerId;

    @Column(length=64, unique = true)
    private String plateNumber;

    @Column(length=128)
    private String brand;

    @Column(length=128)
    private String model;

    private Integer yearOfManufacture;
}
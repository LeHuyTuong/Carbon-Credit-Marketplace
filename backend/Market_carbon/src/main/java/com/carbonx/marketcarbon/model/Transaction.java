package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerCompanyId;

    private Long sellerCompanyId;

    private Long listingId;

    private int amount;

    private BigDecimal totalPrice;

    private LocalDateTime timestamp = LocalDateTime.now();

}

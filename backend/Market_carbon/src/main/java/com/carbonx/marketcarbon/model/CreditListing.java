package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ListingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private Long sellerCompanyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carbon_credit_id")
    private CarbonCredit carbonCredit;

    private int amount; //con lai

    private int amountSold = 0; // da ban

    private BigDecimal pricePerCredit;

    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
}

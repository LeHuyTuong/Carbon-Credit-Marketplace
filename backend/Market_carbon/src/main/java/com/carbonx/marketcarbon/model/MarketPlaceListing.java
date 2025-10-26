package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ListingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "marketplace_listings")
public class MarketPlaceListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id" , nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carbon_credits_id", nullable = false)
    private CarbonCredit carbonCredit;

    @Column(nullable = false, precision = 18, scale = 4, columnDefinition = "DECIMAL(18,4) DEFAULT 0")
    @Builder.Default
    private BigDecimal originalQuantity = BigDecimal.ZERO; // tổng số tín chỉ ban đầu khi niêm yết

    @Column(nullable = false, precision = 18, scale = 4, columnDefinition = "DECIMAL(18,4) DEFAULT 0")
    @Builder.Default
    private BigDecimal soldQuantity = BigDecimal.ZERO; // số lượng đã bán ra khỏi listing này

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity; // amount of credit can sell

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal pricePerCredit; // price for a credit

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.AVAILABLE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Time expires
}

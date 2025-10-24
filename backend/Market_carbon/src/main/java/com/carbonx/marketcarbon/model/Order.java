package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.common.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",  nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private MarketPlaceListing marketplaceListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carbon_credit_id" , nullable = false)
    private CarbonCredit carbonCredit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice; // price a per credit in this order

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal platformFee; // fee trading

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal sellerPayout; // Money company sell to receive

    @CreationTimestamp
    private LocalDateTime createdAt;


}

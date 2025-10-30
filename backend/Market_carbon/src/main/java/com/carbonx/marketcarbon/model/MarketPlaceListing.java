package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ListingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // cơ chế idempotency(Khóa bất biến) để tránh xử lý trùng lặp
    //Idempotency (Tính bất biến)thực hiện 1 vc nhiều lần thì kết quả vẫn y hệt như khi thực hiện chỉ một lần.
    //Mục đích của nó là để biến một hành động không bất biến (như "tạo đơn hàng") thành một hành động bất biến.
    @Column(name = "idempotency_key")
    private String idempotencyKey;

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
    private LocalDate expiresAt; // Time expires
}

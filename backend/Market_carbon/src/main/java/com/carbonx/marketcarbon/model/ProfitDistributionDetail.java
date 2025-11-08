package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity này ghi lại chi tiết từng khoản thanh toán cho mỗi EV Owner
 * trong một đợt chia lợi nhuận.
 */
@Entity
@Table(name = "profit_distribution_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDistributionDetail extends BaseEntity {

    @Id  // <--- KHÓA CHÍNH (PRIMARY KEY) NẰM Ở ĐÂY
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết đến đợt chia lợi nhuận tổng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_id", nullable = false)
    private ProfitDistribution distribution;

    // EV Owner nhận tiền/tín chỉ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ev_owner_id", nullable = false)
    private EVOwner evOwner;

    // Số tiền nhận được
    @Column(nullable = false)
    private BigDecimal moneyAmount;

    // Số tín chỉ nhận được
    @Column(nullable = false)
    private BigDecimal creditAmount;

    // Trạng thái
    @Column(nullable = false)
    private String status;

    // Ghi chú lỗi nếu có
    @Column
    private String errorMessage;
}

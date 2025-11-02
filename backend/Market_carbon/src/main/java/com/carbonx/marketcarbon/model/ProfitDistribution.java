package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.ProfitDistributionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entity này ghi lại thông tin của một đợt chia lợi nhuận tổng thể
 */
@Entity
@Table(name = "profit_distribution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDistribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Công ty/Quản trị viên thực hiện chia sẻ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_user_id", nullable = false)
    private User companyUser;

    // Dự án mà đợt chia sẻ này áp dụng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Tổng số tiền được chia sẻ trong đợt này
    @Column(nullable = false)
    private BigDecimal totalMoneyDistributed;

    // Tổng số tín chỉ được chia sẻ trong đợt này
    @Column(nullable = false)
    private BigDecimal totalCreditsDistributed;

    // Mô tả cho đợt chia sẻ
    @Column(length = 500)
    private String description;

    // Trạng thái của đợt chia sẻ (PENDING, PROCESSING, COMPLETED, FAILED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfitDistributionStatus status;

    // Chi tiết các khoản thanh toán cho từng EV Owner
    @OneToMany(mappedBy = "distribution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProfitDistributionDetail> details;
}

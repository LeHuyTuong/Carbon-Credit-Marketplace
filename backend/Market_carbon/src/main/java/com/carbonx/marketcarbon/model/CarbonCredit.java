package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "carbon_credits")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CarbonCredit extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A unique identifier for this batch of credits, as requested.
    @Column(name = "credit_code", unique = true, nullable = false, length = 64)
    private String creditCode;

    @Column(name = "carbon_credit", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal carbonCredit = BigDecimal.ONE;

    @Column(name = "t_co2e", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal tCo2e = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private CreditBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CreditStatus status = CreditStatus.PENDING;

    private int amount; // số lượng tín chỉ có

    private int listedAmount = 0; // số lượng tín chỉ đang niêm yết

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "issued_by", length = 100)
    private String issuedBy;

    @Column(name = "name", nullable = false, length = 128)
    @Builder.Default
    private String name = "Carbon Credit";

    @JsonProperty("current_price")
    private double currentPrice;


    // Getter to extract the year from issueAt, fulfilling the "năm phát sinh" requirement
    @Transient
    public Integer getVintageYear() {
        return issuedAt != null ? issuedAt.getYear() : null;
    }
}

package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carbon_credits")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CarbonCredit{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carbon_credit_id")
    private Long id;

    // A unique identifier for this batch of credits, as requested.
    @Column(unique = true, nullable = false)
    private String creditCode;

    private BigDecimal carbonCredit; // số lượng tín chỉ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @Enumerated(EnumType.STRING)
    private CreditStatus status = CreditStatus.PENDING;


    private int listedAmount = 0; // số lượng tín chỉ đang niêm yết

    private LocalDateTime issueAt;

    @JsonProperty("name")
    private String name;

    @JsonProperty("current_price")
    private double currentPrice;


    @OneToMany(mappedBy = "carbonCredit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<CarbonCreditContribution> contributions = new ArrayList<>();


    // Getter to extract the year from issueAt, fulfilling the "năm phát sinh" requirement
    @Transient
    @JsonProperty("vintageYear")
    public Integer getVintageYear() {
        return issueAt != null ? issueAt.getYear() : null;
    }



    public void setCarbonCredit(BigDecimal totalCredits) {
        BigDecimal safeTotal = totalCredits == null ? BigDecimal.ZERO : totalCredits;
        if (safeTotal.compareTo(BigDecimal.ZERO) < 0) {
            safeTotal = BigDecimal.ZERO;
        }

        BigDecimal listedPortion = BigDecimal.valueOf(this.listedAmount);
        BigDecimal unlistedPortion = safeTotal.subtract(listedPortion);
        if (unlistedPortion.compareTo(BigDecimal.ZERO) < 0) {
            this.carbonCredit = BigDecimal.ZERO;
            this.listedAmount = safeTotal.setScale(0, RoundingMode.DOWN).intValue();
        } else {
            this.carbonCredit = unlistedPortion;
        }
    }
}

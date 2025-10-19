package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // Getter to extract the year from issueAt, fulfilling the "năm phát sinh" requirement
    @Transient
    @JsonProperty("vintageYear")
    public Integer getVintageYear() {
        return issueAt != null ? issueAt.getYear() : null;
    }
}

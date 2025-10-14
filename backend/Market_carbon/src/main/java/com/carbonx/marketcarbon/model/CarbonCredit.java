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

    private BigDecimal carbonCredit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @Enumerated(EnumType.STRING)
    private CreditStatus status;

    private int amount; // số lượng tín chỉ có

    private int listedAmount = 0; // số lượng tín chỉ đang niêm yết

    private LocalDateTime issueAt;

    @JsonProperty("name")
    private String name;

    @JsonProperty("current_price")
    private double currentPrice;

}

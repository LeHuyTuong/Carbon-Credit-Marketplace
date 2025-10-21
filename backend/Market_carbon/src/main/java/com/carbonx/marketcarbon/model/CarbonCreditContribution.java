package com.carbonx.marketcarbon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "carbon_credit_contributions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_credit_owner", columnNames = {"carbon_credit_id", "ev_owner_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCreditContribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carbon_credit_id", nullable = false)
    @JsonIgnore
    private CarbonCredit carbonCredit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private EVOwner evOwner;

    @Column(name = "contributed_credits", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal contributedCredits = BigDecimal.ZERO;

    @Column(name = "payout_ratio", precision = 6, scale = 4)
    private BigDecimal payoutRatio;
}

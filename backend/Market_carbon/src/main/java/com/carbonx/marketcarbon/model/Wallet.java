package com.carbonx.marketcarbon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "carbon_credit_id", referencedColumnName = "id")
    private CarbonCredit carbonCredit;

    @OneToOne
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @OneToMany(mappedBy = "wallet",
            cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<WalletTransaction> walletTransactions = new ArrayList<>();


    private BigDecimal balance =  BigDecimal.ZERO; // for currency

    @Column(precision = 18, scale = 4)
    private BigDecimal carbonCreditBalance = BigDecimal.ZERO; // For carbon credits
}

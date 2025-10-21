package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.CarbonCreditContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarbonCreditContributionRepository extends JpaRepository<CarbonCreditContribution, Long> {
    List<CarbonCreditContribution> findByCarbonCredit(CarbonCredit carbonCredit);

}

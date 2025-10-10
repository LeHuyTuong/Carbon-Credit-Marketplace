package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.model.ChargingData;

import java.math.BigDecimal;
import java.util.List;


public interface CarbonCreditService {
    BigDecimal calculateCarbonCredit(BigDecimal chargingEnergy);

    void issueCredits(List<ChargingData> approveData);
}

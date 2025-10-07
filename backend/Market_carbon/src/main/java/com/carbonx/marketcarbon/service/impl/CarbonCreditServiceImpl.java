package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.model.ChargingData;
import com.carbonx.marketcarbon.service.CarbonCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarbonCreditServiceImpl implements CarbonCreditService {

    @Override
    public BigDecimal calculateCarbonCredit(BigDecimal chargingEnergy) {
        BigDecimal emissionReductionKg = chargingEnergy.multiply(new BigDecimal("0.5"));
        return emissionReductionKg.divide(new BigDecimal("1000"), RoundingMode.HALF_UP);
    }

    @Override
    public void issueCredits(List<ChargingData> approveData) {

    }


}

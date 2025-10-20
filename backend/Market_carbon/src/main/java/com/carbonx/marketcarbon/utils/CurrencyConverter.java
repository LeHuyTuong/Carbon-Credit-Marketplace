package com.carbonx.marketcarbon.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyConverter {

    private static final BigDecimal USD_TO_VND_RATE = new BigDecimal("26000");

    public CurrencyConverter() {
    }

    public static BigDecimal usdToVnd(BigDecimal usdAmount) {
        if (usdAmount == null){
            throw new IllegalArgumentException("USD amount cannot be null");
        }
        return usdAmount.multiply(USD_TO_VND_RATE).setScale(0, RoundingMode.HALF_DOWN);
    }

    public static BigDecimal getUsdToVndRate() {
        return USD_TO_VND_RATE;
    }
}

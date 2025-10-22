package com.carbonx.marketcarbon.service.credit.formula;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.Project;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class DefaultCreditFormula implements CreditFormula {

    private static final RoundingMode RM = RoundingMode.DOWN;

    @Override
    public CreditComputationResult compute(EmissionReport report, Project project) {
        // 1) Lấy CO2 kg (ưu tiên report.totalCo2; fallback = totalEnergy * EF dự án)
        BigDecimal co2kg = Optional.ofNullable(report.getTotalCo2())
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .orElseGet(() -> {
                    BigDecimal ef = Optional.ofNullable(project.getEmissionFactorKgPerKwh())
                            .orElse(new BigDecimal("0.4000")); // fallback 0.4 kg/kWh
                    return report.getTotalEnergy().multiply(ef);
                });

        // 2) kg → tCO2e (6 chữ số, rồi dùng tiếp)
        BigDecimal grossT = co2kg.divide(new BigDecimal("1000"), 6, RM);

        // 3) Áp dụng các hệ số trừ
        BigDecimal buffer = nz(project.getBufferReservePct());
        BigDecimal uncrt  = nz(project.getUncertaintyPct());
        BigDecimal leak   = nz(project.getLeakagePct());

        BigDecimal multiplier = BigDecimal.ONE
                .subtract(buffer)
                .subtract(uncrt)
                .subtract(leak);

        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            multiplier = BigDecimal.ZERO;
        }

        // 4) Kết quả tCO2e sau điều chỉnh (3 chữ số)
        BigDecimal netT = grossT.multiply(multiplier).setScale(3, RM);

        // 5) floor tín chỉ + phần dư
        int credits = netT.setScale(0, RM).intValueExact();
        BigDecimal residual = netT.subtract(BigDecimal.valueOf(credits)).setScale(3, RM);

        return new CreditComputationResult(netT, credits, residual);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
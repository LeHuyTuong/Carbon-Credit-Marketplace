package com.carbonx.marketcarbon.service.credit.formula;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCreditFormula implements CreditFormula {

    private static final RoundingMode RM = RoundingMode.DOWN;
    private final EmissionReportRepository reportRepo;

    @Override
    public CreditComputationResult compute(EmissionReport report, Project project) {
        // Lấy phần dư kỳ trước nếu có
        BigDecimal prevResidual = reportRepo
                .findTopByProjectAndSellerAndPeriodBeforeOrderByPeriodDesc(
                        project, report.getSeller(), report.getPeriod())
                .map(EmissionReport::getResidualTco2e)
                .orElse(BigDecimal.ZERO);

        if (prevResidual.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Previous residual {} tCO2e for project={}, company={}, period={}",
                    prevResidual, project.getTitle(), report.getSeller().getCompanyName(), report.getPeriod());
        }

        // Lấy CO2 (kg) của kỳ hiện tại
        BigDecimal co2kg = Optional.ofNullable(report.getTotalCo2())
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .orElseGet(() -> {
                    BigDecimal ef = Optional.ofNullable(project.getEmissionFactorKgPerKwh())
                            .orElse(new BigDecimal("0.4000"));
                    return report.getTotalEnergy().multiply(ef);
                });

        // Chuyển đổi sang tCO2e
        BigDecimal grossT = co2kg.divide(new BigDecimal("1000"), 6, RM);

        // Áp dụng các hệ số trừ
        BigDecimal buffer = nz(project.getBufferReservePct());
        BigDecimal uncrt  = nz(project.getUncertaintyPct());
        BigDecimal leak   = nz(project.getLeakagePct());

        BigDecimal multiplier = BigDecimal.ONE
                .subtract(buffer)
                .subtract(uncrt)
                .subtract(leak);
        if (multiplier.compareTo(BigDecimal.ZERO) < 0)
            multiplier = BigDecimal.ZERO;

        // Tính tổng tCO2e sau điều chỉnh và cộng phần dư kỳ trước
        BigDecimal netT = grossT.multiply(multiplier)
                .add(prevResidual)
                .setScale(3, RM);

        // Làm tròn xuống để tính tín chỉ và phần dư mới
        int credits = netT.setScale(0, RM).intValueExact();
        BigDecimal residual = netT.subtract(BigDecimal.valueOf(credits)).setScale(3, RM);

        log.info("Computed total={} tCO2e, credits={}, residual={}", netT, credits, residual);

        return new CreditComputationResult(netT, credits, residual);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

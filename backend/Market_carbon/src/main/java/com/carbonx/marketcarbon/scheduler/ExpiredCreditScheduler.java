package com.carbonx.marketcarbon.scheduler;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredCreditScheduler {

    private final CarbonCreditRepository creditRepo;

    @Scheduled(cron = "0 0 2 * * *") // chạy lúc 2h sáng mỗi ngày
    public void markExpiredCredits() {
        List<CarbonCredit> credits = creditRepo.findByStatusNot(CreditStatus.EXPIRED);
        credits.stream()
                .filter(c -> c.getExpiryDate() != null && c.getExpiryDate().isBefore(LocalDate.now()))
                .forEach(c -> {
                    c.setStatus(CreditStatus.EXPIRED);
                    creditRepo.save(c);
                    log.info("[SCHEDULER] Marked credit {} as EXPIRED", c.getCreditCode());
                });
    }
}

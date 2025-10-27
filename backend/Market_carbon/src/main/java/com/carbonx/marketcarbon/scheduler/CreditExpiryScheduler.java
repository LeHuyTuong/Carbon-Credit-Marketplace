package com.carbonx.marketcarbon.scheduler;

import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditExpiryScheduler {

    private final CarbonCreditRepository creditRepo;

    // Chạy lúc 02:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoMarkExpiredCredits() {
        log.info("[Scheduler] Checking expired carbon credits...");
        int updated = creditRepo.markExpiredCredits();
        log.info("[Scheduler] {} credits marked as EXPIRED.", updated);
    }
}

package com.carbonx.marketcarbon.scheduler;


import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    // Chạy mỗi 5 phút
    @Scheduled(cron = "0 0/5 * * * *") // Mỗi phút thứ 0,5,10,...
    public void deleteExpiredPendingUsers() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = userRepository.deleteByStatusAndOtpExpiryDateBefore(USER_STATUS.PENDING, now);
        if (deletedCount > 0) {
            log.info("🧹 Đã xóa {} user chưa xác thực OTP (đã hết hạn).", deletedCount);
        }
    }
}

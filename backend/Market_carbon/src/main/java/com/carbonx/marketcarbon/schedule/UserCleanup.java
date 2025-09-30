package com.carbonx.marketcarbon.schedule;

import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCleanup {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 300000) // 5 phút (ms)
    public void deleteExpiredPendingUsers() {
        var now = java.time.OffsetDateTime.now();
        userRepository.deleteByStatusAndOtpExpiredAtBefore(USER_STATUS.PENDING, now);
    }
}

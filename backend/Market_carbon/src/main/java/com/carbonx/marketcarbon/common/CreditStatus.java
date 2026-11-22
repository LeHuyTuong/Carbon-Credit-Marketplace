package com.carbonx.marketcarbon.common;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Set;

public enum CreditStatus {
    AVAILABLE,
    ISSUE,
    ISSUED, // admin cấp phát
    TRADED, // tín chỉ được để lên sàn
    PENDING,
    EXPIRED,
    SOLD,
    LISTED,
    RETIRED;

    private static final Set<CreditStatus> TERMINAL_STATUSES = EnumSet.of(RETIRED, EXPIRED);

    /**
     * Credit chưa bị hưu/expire có thể chuyển sang trạng thái khác (kể cả RETIRED/EXPIRED).
     * RETIRED và EXPIRED là trạng thái kết thúc, không chuyển tiếp được nữa.
     */
    public boolean canTransitionTo(CreditStatus target) {
        return target != null
                && this != target
                && !TERMINAL_STATUSES.contains(this);
    }

    /** Ném lỗi khi chuyển trạng thái không hợp lệ. */
    public void requireTransitionTo(CreditStatus target) {
        if (!canTransitionTo(target)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

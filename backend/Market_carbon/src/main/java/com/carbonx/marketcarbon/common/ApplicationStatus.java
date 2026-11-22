package com.carbonx.marketcarbon.common;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;

public enum ApplicationStatus {
    SUBMITTED,          // Đã nộp đơn
    UNDER_REVIEW,       // CVA đang thẩm định
    CVA_APPROVED,
    CVA_REJECTED,
    NEEDS_REVISION,
    ADMIN_APPROVED,           // Admin duyệt cuối
    ADMIN_REJECTED;

    /**
     * Các chuyển trạng thái hợp lệ. ADMIN_APPROVED/ADMIN_REJECTED là trạng thái kết thúc.
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case SUBMITTED -> target == UNDER_REVIEW;
            case UNDER_REVIEW -> target == CVA_APPROVED
                    || target == CVA_REJECTED
                    || target == NEEDS_REVISION;
            case NEEDS_REVISION -> target == UNDER_REVIEW;
            case CVA_APPROVED -> target == ADMIN_APPROVED || target == ADMIN_REJECTED;
            case CVA_REJECTED, ADMIN_APPROVED, ADMIN_REJECTED -> false;
        };
    }

    /** Ném lỗi khi chuyển trạng thái không hợp lệ. */
    public void requireTransitionTo(ApplicationStatus target) {
        if (!canTransitionTo(target)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

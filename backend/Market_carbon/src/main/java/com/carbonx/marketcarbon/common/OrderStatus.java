package com.carbonx.marketcarbon.common;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;

public enum OrderStatus {
    PENDING,
    FINISHED,
    CANCELLED,
    PARTIALLY_FILLED,
    ERROR,
    SUCCESS;

    /**
     * Chỉ đơn PENDING được phép chuyển tiếp; SUCCESS/CANCELLED và các trạng thái còn lại
     * là trạng thái kết thúc.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == SUCCESS || target == CANCELLED || target == ERROR;
            case FINISHED, PARTIALLY_FILLED, ERROR, SUCCESS, CANCELLED -> false;
        };
    }

    /** Ném lỗi khi chuyển trạng thái không hợp lệ. */
    public void requireTransitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

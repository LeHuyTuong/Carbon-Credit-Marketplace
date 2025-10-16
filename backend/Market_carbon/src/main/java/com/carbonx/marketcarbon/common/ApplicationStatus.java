package com.carbonx.marketcarbon.common;

public enum ApplicationStatus {
    SUBMITTED,          // Đã nộp đơn
    UNDER_REVIEW,       // CVA đang thẩm định
    CVA_APPROVED,
    CVA_REJECTED,
    NEEDS_REVISION,
    ADMIN_APPROVED,           // Admin duyệt cuối
    ADMIN_REJECTED
}

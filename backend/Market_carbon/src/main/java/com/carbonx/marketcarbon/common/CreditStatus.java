package com.carbonx.marketcarbon.common;

public enum CreditStatus {
    AVAILABLE,
    PENDING,  // đợi cva duyệt
    APPROVED, // cva duyệt
    ISSUE, // admin cấp phát
    REJECTED, // cva từ chối thì thôi luôn
    TRADED // tín chỉ được để lên sàn
}

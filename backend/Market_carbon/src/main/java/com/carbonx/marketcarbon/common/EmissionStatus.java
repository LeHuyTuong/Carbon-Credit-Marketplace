package com.carbonx.marketcarbon.common;


public enum EmissionStatus {
    DRAFT,
    SUBMITTED,        // Seller nộp
    CVA_APPROVED,     // CVA duyệt chuyên môn
    ADMIN_APPROVED,   // Admin phê duyệt cuối
    REJECTED          // Bị từ chối ở bất kỳ bước nào
}
package com.carbonx.marketcarbon.common;


public enum EmissionStatus {
    DRAFT,
    SUBMITTED,        // Seller nộp
    CVA_APPROVED,
    CVA_REJECTED,
    ADMIN_APPROVED,
    ADMIN_REJECTED,
    CREDIT_ISSUED,
    REJECTED,
    APPROVED,
    PAID_OUT // Thêm trạng thái này để đánh dấu report đã được thanh toán
}

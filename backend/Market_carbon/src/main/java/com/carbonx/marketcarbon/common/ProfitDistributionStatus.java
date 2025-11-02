package com.carbonx.marketcarbon.common;

public enum ProfitDistributionStatus {
    PENDING, // Đang chờ xử lý
    PROCESSING, // Đang trong quá trình xử lý (concurrency)
    COMPLETED, // Hoàn thành
    FAILED // Thất bại (có lỗi xảy ra)
}

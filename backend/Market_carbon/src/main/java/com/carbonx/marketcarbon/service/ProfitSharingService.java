package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;

public interface ProfitSharingService {

    /**
     * Bắt đầu quá trình chia sẻ lợi nhuận.
     * Phương thức này sẽ chạy bất đồng bộ (async) — qualifier nằm ở implementation.
     *
     * @param companyUserId ID của company user đã được xác thực ở controller
     * @param request       Thông tin về đợt chia sẻ (tổng tiền, tổng tín chỉ, ...)
     */
    void shareCompanyProfit(Long companyUserId, ProfitSharingRequest request);

    /**
     * Trả lại tiền cho các owner còn FAILED trong một đợt phân phối đã chạy dở.
     * Chỉ xử lý các detail FAILED của đúng đợt đó, không tính lại toàn bộ report,
     * nên các owner đã nhận tiền không bị trả hai lần.
     *
     * @param companyUserId  ID của company user đã được xác thực ở controller
     * @param distributionId ID đợt phân phối cần retry
     */
    void retryFailedPayouts(Long companyUserId, Long distributionId);
}

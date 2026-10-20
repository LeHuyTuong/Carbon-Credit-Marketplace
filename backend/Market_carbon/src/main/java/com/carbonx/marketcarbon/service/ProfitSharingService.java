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

}

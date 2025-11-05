package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import org.springframework.scheduling.annotation.Async;

public interface ProfitSharingService {

    /**
     * Bắt đầu quá trình chia sẻ lợi nhuận.
     * Phương thức này sẽ chạy bất đồng bộ (async).
     *
     * @param request      Thông tin về đợt chia sẻ (tổng tiền, tổng tín chỉ, ...)
     */
    @Async("profitSharingTaskExecutor")
    void shareCompanyProfit(ProfitSharingRequest request);

}

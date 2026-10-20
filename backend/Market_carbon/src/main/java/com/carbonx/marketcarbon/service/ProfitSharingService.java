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
     * Validate + claim quyền retry NGAY trên request thread, sau đó mới chạy payout
     * bất đồng bộ. Nhờ vậy id không tồn tại, distribution của công ty khác, distribution
     * đã COMPLETED hay đang chạy đều trả lỗi 400/403/404 cho client thay vì 200 + log.
     *
     * <p>Chỉ các detail PENDING/FAILED của đúng đợt được trả lại; owner đã SUCCESS
     * không bị trả hai lần. Distribution cũ không còn là bản mới nhất của report sẽ
     * bị từ chối để không trả trùng với đợt mới hơn.</p>
     *
     * @param companyUserId  ID của company user đã được xác thực ở controller
     * @param distributionId ID đợt phân phối cần retry
     */
    void requestRetry(Long companyUserId, Long distributionId);
}

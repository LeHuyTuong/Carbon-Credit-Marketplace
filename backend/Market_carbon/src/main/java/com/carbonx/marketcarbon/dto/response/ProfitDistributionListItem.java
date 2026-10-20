package com.carbonx.marketcarbon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Một đợt phân phối lợi nhuận của công ty, gọn đủ để UI biết đợt nào đang chạy,
 * đợt nào đã xong và đợt nào còn owner chưa nhận được tiền (để bấm "Gửi lại").
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDistributionListItem {

    private Long id;
    private String status;
    private Long emissionReportId;
    private String reportPeriod;
    private BigDecimal totalMoneyDistributed;
    /** Số owner CHƯA được trả của đợt này. */
    private long unsettledOwners;
    private OffsetDateTime createdAt;
    /** Đợt này còn owner chưa nhận tiền thì có thể retry. */
    private boolean retryable;
}

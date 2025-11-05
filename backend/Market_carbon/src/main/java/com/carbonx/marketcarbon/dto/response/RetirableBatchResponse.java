package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.CreditBatch;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO tóm tắt thông tin một Lô (Batch) có tín chỉ
 * sẵn sàng để nghỉ hưu (Retire).
 */
@Data
@Builder
public class RetirableBatchResponse {
    private Long batchId;
    private String batchCode;
    private Long projectId;
    private String projectTitle;
    private Integer vintageYear;
    private LocalDate expiryDate;
    private BigDecimal totalAvailableAmount; // Tổng số lượng có thể retire từ batch này

    /**
     * Factory method để tạo DTO từ CreditBatch và tổng số lượng có thể retire
     * @param batch Lô tín chỉ
     * @param totalAvailable Tổng số lượng có thể retire đã tính toán
     * @return RetirableBatchResponse
     */
    public static RetirableBatchResponse from(CreditBatch batch, BigDecimal totalAvailable) {
        if (batch == null) {
            return null;
        }
        return RetirableBatchResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .projectId(batch.getProject() != null ? batch.getProject().getId() : null)
                .projectTitle(batch.getProject() != null ? batch.getProject().getTitle() : null)
                .vintageYear(batch.getVintageYear())
                .expiryDate(batch.getExpiresAt())
                .totalAvailableAmount(totalAvailable)
                .build();
    }
}

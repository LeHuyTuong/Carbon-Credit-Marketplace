package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.common.Status;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // ẩn toàn bộ những field null
public class PaymentOrderResponse {
    private Long id;
    private Long amount;
    private Status status;
    private PaymentMethod method;
    private Long userId;
    private OffsetDateTime createdDate;
    private String payment_url;

    public PaymentOrderResponse(String payment_url) {
        this.payment_url = payment_url;
    }
}

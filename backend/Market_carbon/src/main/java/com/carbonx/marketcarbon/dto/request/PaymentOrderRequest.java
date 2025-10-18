package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderRequest {
    private Long amount;
    private PaymentMethod paymentMethod;
}

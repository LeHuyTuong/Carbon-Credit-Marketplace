package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.Wallet;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletTransactionRequest {
    private Wallet wallet;
    private Order order;
    private PaymentOrder paymentOrder;
    private WalletTransactionType type;
    private String description;
    private BigDecimal amount;
}

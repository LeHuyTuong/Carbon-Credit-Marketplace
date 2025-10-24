package com.carbonx.marketcarbon.dto.response;


import com.carbonx.marketcarbon.common.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CreditTradeResponse {

    private Long id;

    private Long companyId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private LocalDateTime createAt = LocalDateTime.now();
}

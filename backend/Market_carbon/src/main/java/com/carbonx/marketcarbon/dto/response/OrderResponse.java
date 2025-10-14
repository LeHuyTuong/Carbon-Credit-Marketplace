package com.carbonx.marketcarbon.dto.response;


import com.carbonx.marketcarbon.common.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long id;

    private Long companyId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private LocalDateTime createAt = LocalDateTime.now();
}

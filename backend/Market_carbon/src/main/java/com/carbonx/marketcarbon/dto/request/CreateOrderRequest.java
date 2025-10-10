package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.OrderType;
import com.carbonx.marketcarbon.model.OrderItem;
import lombok.Data;

@Data
public class CreateOrderRequest {
    private String orderId;
    private OrderItem orderItem;
    private double quantity;
    private OrderType orderType;
}

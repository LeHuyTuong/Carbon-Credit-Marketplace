package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request );

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getUserOrders();

    void cancelOrder(Long orderId);

    void completeOrder(Long orderId);
}

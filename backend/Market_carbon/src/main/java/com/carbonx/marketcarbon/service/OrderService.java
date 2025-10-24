package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.response.CreditTradeResponse;

import java.util.List;

public interface OrderService {

    CreditTradeResponse createOrder(OrderRequest request );

    CreditTradeResponse getOrderById(Long orderId);

    List<CreditTradeResponse> getUserOrders();

    void cancelOrder(Long orderId);

    void completeOrder(Long orderId);
}

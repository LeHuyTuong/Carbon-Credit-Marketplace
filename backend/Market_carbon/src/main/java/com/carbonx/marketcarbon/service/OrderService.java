package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.Order;

public interface OrderService {

    Order createOrder(Order order);

    Order getOrderById(Long orderId);


}

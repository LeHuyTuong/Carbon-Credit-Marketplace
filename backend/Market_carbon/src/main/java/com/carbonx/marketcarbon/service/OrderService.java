package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.CreateOrderRequest;
import com.carbonx.marketcarbon.model.Order;
import org.hibernate.mapping.List;

public interface OrderService {

    Order createOrder(CreateOrderRequest request );

    Order getOrderById(Long orderId);

//    List<Order> getAllOrders();

}

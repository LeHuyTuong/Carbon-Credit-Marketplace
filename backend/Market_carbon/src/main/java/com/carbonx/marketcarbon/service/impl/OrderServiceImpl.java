package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.dto.request.CreateOrderRequest;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.OrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request ) {
        User user = currentUser();

        double price = request.getOrderItem().getCarbonCredit().getCurrentPrice() * request.getOrderItem().getQuantity();

        Order order = Order.builder()
                .user(user)
                .orderItem(request.getOrderItem())
                .orderType(request.getOrderType())
                .price(BigDecimal.valueOf(price))
                .createdAt(LocalDateTime.now())
                .orderStatus(OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(
                () -> new IllegalArgumentException("Order Not Found!"));
    }
}

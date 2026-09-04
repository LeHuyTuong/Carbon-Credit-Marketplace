package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * P0-B/B4: persists the ERROR state transition of a failed settlement in its OWN
 * transaction, AFTER the settlement transaction has rolled back.
 *
 * Why a separate bean (not a method inside OrderServiceImpl):
 *  - @Transactional works through Spring proxies; a self-invoked method call bypasses
 *    the proxy, so REQUIRES_NEW would silently not apply.
 * Why it is called from the controller's catch, not from inside completeOrder:
 *  - completeOrder holds a pessimistic lock on the order row (B3). A nested
 *    REQUIRES_NEW transaction trying to UPDATE that same row would wait for the
 *    outer (suspended, never-committing) transaction to release it — a guaranteed
 *    self-deadlock until innodb_lock_wait_timeout. Only after the outer transaction
 *    has fully rolled back (exception propagated to the caller) is the row lock free
 *    and the ERROR write safe.
 *
 * Guard: only a PENDING order may transition to ERROR — a concurrently completed
 * (SUCCESS) order is never overwritten.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusRecorder {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderError(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                order.setOrderStatus(OrderStatus.ERROR);
                orderRepository.save(order);
                log.warn("Order {} marked ERROR after failed settlement: {}", orderId, reason);
            } else {
                log.info("Order {} not marked ERROR (current status={})", orderId, order.getOrderStatus());
            }
        });
    }
}

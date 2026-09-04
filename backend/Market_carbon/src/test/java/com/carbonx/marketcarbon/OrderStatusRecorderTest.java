package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.repository.OrderRepository;
import com.carbonx.marketcarbon.service.impl.OrderStatusRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B/B4: a failed settlement must leave the order in ERROR, persisted in a
 * transaction independent of the rolled-back settlement. Before B4 the ERROR write
 * happened inside the failing transaction and was discarded — orders stayed PENDING forever.
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusRecorderTest {

    @Mock private OrderRepository orderRepository;

    private OrderStatusRecorder recorder;

    @Test
    void markError_pendingOrder_transitionsToErrorAndPersists() {
        recorder = new OrderStatusRecorder(orderRepository);
        Order order = Order.builder()
                .id(1L).orderStatus(OrderStatus.PENDING)
                .quantity(BigDecimal.ONE).totalPrice(BigDecimal.TEN)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        recorder.markOrderError(1L, "insufficient funds");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ERROR);
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderStatus()).isEqualTo(OrderStatus.ERROR);
    }

    @Test
    void markError_alreadySucceededOrder_neverDowngraded() {
        recorder = new OrderStatusRecorder(orderRepository);
        Order order = Order.builder()
                .id(1L).orderStatus(OrderStatus.SUCCESS)
                .quantity(BigDecimal.ONE).totalPrice(BigDecimal.TEN)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        recorder.markOrderError(1L, "late failure");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void markError_unknownOrder_isNoOp() {
        recorder = new OrderStatusRecorder(orderRepository);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        recorder.markOrderError(99L, "missing");

        verify(orderRepository, never()).save(any());
    }
}

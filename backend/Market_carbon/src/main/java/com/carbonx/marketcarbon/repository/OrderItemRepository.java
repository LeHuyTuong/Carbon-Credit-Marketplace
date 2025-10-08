package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
}

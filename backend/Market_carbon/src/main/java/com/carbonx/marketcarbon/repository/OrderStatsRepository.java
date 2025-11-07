package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderStatsRepository extends JpaRepository<Order, Long> {

    @Query("""
        select avg(o.unitPrice) 
        from Order o  
        join o.marketplaceListing l
        where (o.company.id = :companyId or l.company.id = :companyId)
          and o.orderStatus = com.carbonx.marketcarbon.common.OrderStatus.SUCCESS
          and coalesce(o.completedAt, o.createdAt) between :from and :to
    """)
    BigDecimal avgPrice(Long companyId, LocalDateTime from, LocalDateTime to);

    @Query("""
        select min(o.unitPrice) 
        from Order o
        join o.marketplaceListing l
        where (o.company.id = :companyId or l.company.id = :companyId)
          and o.orderStatus = com.carbonx.marketcarbon.common.OrderStatus.SUCCESS
          and coalesce(o.completedAt, o.createdAt) between :from and :to
    """)
    BigDecimal minPrice(Long companyId, LocalDateTime from, LocalDateTime to);

    @Query("""
        select max(o.unitPrice) 
        from Order o
        join o.marketplaceListing l
        where (o.company.id = :companyId or l.company.id = :companyId)
          and o.orderStatus = com.carbonx.marketcarbon.common.OrderStatus.SUCCESS
          and coalesce(o.completedAt, o.createdAt) between :from and :to
    """)
    BigDecimal maxPrice(Long companyId, LocalDateTime from, LocalDateTime to);
}

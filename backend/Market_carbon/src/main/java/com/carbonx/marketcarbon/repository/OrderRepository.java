package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByCompany(Company companyBuyer);

    // Eager-load company, marketplaceListing (with its company and carbonCredit),
    // and carbonCredit in a single query to eliminate N+1 lazy loads
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.company c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH o.marketplaceListing ml " +
           "LEFT JOIN FETCH ml.company mlc " +
           "LEFT JOIN FETCH ml.carbonCredit " +
           "LEFT JOIN FETCH o.carbonCredit " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);
}

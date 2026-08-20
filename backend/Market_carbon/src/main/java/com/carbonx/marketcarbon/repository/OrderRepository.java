package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // P1.3: single-statement guarded read — plain SELECT ... FOR UPDATE (no fetch joins,
    // so the lock is inline, not follow-on). This is the only lock callers may use for
    // read-status-then-decide logic.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithPessimisticLock(@Param("id") Long id);

    // WARNING (P1.3, proven by OrderSettlementConcurrencyIT): @Lock + JOIN FETCH degrades to
    // Hibernate follow-on locking — the SELECT reads WITHOUT a lock, rows are locked after.
    // Do NOT use for guards that must read-then-decide. Use findById(id, PESSIMISTIC_WRITE).
    // Retained for other read paths only.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.company c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH o.marketplaceListing ml " +
           "LEFT JOIN FETCH ml.company mlc " +
           "LEFT JOIN FETCH ml.carbonCredit " +
           "LEFT JOIN FETCH o.carbonCredit " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithPessimisticLockAndDetails(@Param("id") Long id);
}

package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Cva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvaRepository extends JpaRepository<Cva, Long> {
    Optional<Cva> findByUserId(Long userId);
    Optional<Cva> findByEmail(String email);
    boolean existsByUserId(Long userId);
}
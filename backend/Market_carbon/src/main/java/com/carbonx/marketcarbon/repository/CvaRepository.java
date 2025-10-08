package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Cva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvaRepository extends JpaRepository<Cva, Long> {
    Optional<Cva> findByUser_Id(Long userId);
    Optional<Cva> findByCode(String code);
}

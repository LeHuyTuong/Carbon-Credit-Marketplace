package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUser_Id(Long userId);
    Optional<Admin> findByCode(String code);
}

package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUserId(Long userId);          // tìm theo user_id
    Optional<Admin> findByUserEmail(String email);      // tìm theo user.email
}

package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.KycProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository extends JpaRepository<KycProfile,Long> {
    Optional<KycProfile> findByEmail(String email); // tìm thông tin dựa trên email
    Optional<KycProfile> findByPhone(String phone); // tìm thông tin dựa trên  sddt

    Optional<KycProfile> findByUserId(Long userId);
}

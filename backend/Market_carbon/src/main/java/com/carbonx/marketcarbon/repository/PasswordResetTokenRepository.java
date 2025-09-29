package com.carbonx.marketcarbon.repository;


import com.carbonx.marketcarbon.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
        PasswordResetToken findByToken(String token);
    }


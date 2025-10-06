package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EVOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EVOwnerRepository extends JpaRepository<EVOwner,Long> {
    Optional<EVOwner> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}

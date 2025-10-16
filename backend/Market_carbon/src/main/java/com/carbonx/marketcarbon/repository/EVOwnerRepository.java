package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.EVOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EVOwnerRepository extends JpaRepository<EVOwner,Long> {
    Optional<EVOwner> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<EVOwner> findByCompanyId(Long companyId);
    @Query("""
        SELECT e FROM EVOwner e
        JOIN FETCH e.user u
        WHERE u.email = :email
    """)
    Optional<EVOwner> findByEmail(String email);
}

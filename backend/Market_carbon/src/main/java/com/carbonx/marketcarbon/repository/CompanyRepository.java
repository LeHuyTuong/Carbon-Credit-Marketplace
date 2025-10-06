package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Long> {
    Optional<Company> findByUserEmail(String email);
}

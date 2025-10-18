package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // Tìm công ty qua email user
    Company findByUserEmail(String email);

    boolean existsById(Long companyId);

    // Tìm công ty qua userId
    @Query("SELECT c FROM Company c WHERE c.user.id = :userId")
    Optional<Company> findByUserId(@Param("userId") Long userId);

    @Query("""
           SELECT DISTINCT c
           FROM Company c
           JOIN c.applications a
           JOIN a.project p
           WHERE p.status IN :statuses
           """)
    Page<Company> findCompaniesHavingProjectsIn(
            @Param("statuses") Collection<ProjectStatus> statuses,
            Pageable pageable
    );

    Optional<Company> findByCompanyNameIgnoreCase(String name);
}
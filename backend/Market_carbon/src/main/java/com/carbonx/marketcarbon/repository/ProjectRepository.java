package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    boolean existsByTitle(String title);
    @Query("SELECT p FROM Project p JOIN FETCH p.company WHERE p.id = :id")
    Optional<Project> findByIdWithCompany(@Param("id") Long id);
}

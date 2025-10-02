package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    boolean existsByTitle(String title);
}

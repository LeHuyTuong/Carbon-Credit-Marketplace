package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.Project;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByTitle(String title);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Optional<Project> findById(Long id);

    Optional<Project> findByTitle(String title);
}

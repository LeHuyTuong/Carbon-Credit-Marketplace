package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.Project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByTitle(String title);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Optional<Project> findById(Long id);

    Optional<Project> findByTitle(String title);
}

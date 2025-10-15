package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    boolean existsByTitle(String title);
    @Query("SELECT p FROM Project p JOIN FETCH p.company WHERE p.id = :id")
    Optional<Project> findByIdWithCompany(@Param("id") Long id);

    @Query("SELECT p FROM Project p WHERE p.status = 'CVA_APPROVED'")
    Page<Project> findAllCvaApproved(Pageable pageable);
    Page<Project> findByReviewerAndStatusIn(String reviewer, List<ProjectStatus> statuses, Pageable pageable);

}

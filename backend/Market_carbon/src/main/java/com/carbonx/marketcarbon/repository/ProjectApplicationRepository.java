package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.ProjectApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    boolean existsByCompanyAndProject(Company company, Project project);

    List<ProjectApplication> findByCompany_User_EmailOrderBySubmittedAtDesc(String email);
    List<ProjectApplication> findByCompany_User_EmailAndStatusOrderBySubmittedAtDesc(String email, ApplicationStatus status);
    List<ProjectApplication> findByStatusOrderBySubmittedAtDesc(ApplicationStatus status);
    Page<ProjectApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    boolean existsByCompanyIdAndProjectId(Long companyId, Long projectId);
    boolean existsByCompanyAndProjectAndStatusIn(
            Company company,
            Project project,
            List<ApplicationStatus> statuses
    );

}

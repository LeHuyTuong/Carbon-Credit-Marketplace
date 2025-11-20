package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.ProjectApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    boolean existsByCompanyAndProject(Company company, Project project);

    List<ProjectApplication> findByCompany_User_EmailOrderBySubmittedAtDesc(String email);
    List<ProjectApplication> findByCompany_User_EmailAndStatusOrderBySubmittedAtDesc(String email, ApplicationStatus status);
    List<ProjectApplication> findByStatusOrderBySubmittedAtDesc(ApplicationStatus status);
    Page<ProjectApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    boolean existsByCompanyIdAndProjectId(Long companyId, Long projectId);
    List<ProjectApplication> findByStatusInOrderBySubmittedAtDesc(List<ApplicationStatus> statuses);
    boolean existsByCompanyAndProjectAndStatusIn(
            Company company,
            Project project,
            List<ApplicationStatus> statuses
    );

    @Query("SELECT COUNT(p) FROM ProjectApplication p")
    long countAllProjects();

    @Query(value =
            "SELECT " +
                    "DATE_FORMAT(submitted_at, '%Y-%m') AS month, " +
                    "SUM(CASE WHEN status = 'UNDER_REVIEW' THEN 1 ELSE 0 END) AS submitted, " +
                    "SUM(CASE WHEN status IN ('CVA_APPROVED', 'ADMIN_APPROVED') THEN 1 ELSE 0 END) AS approved, " +
                    "SUM(CASE WHEN status IN ('CVA_REJECTED', 'ADMIN_REJECTED') THEN 1 ELSE 0 END) AS rejected " +
                    "FROM project_application " +
                    "GROUP BY DATE_FORMAT(submitted_at, '%Y-%m') " +
                    "ORDER BY DATE_FORMAT(submitted_at, '%Y-%m')",
            nativeQuery = true)
    List<Object[]> countMonthlyProjectStatusNative();

}

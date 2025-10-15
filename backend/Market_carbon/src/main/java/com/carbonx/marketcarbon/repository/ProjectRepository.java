package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.model.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    boolean existsByTitle(String title);
    boolean existsByCompanyIdAndParentProjectId(Long companyId, Long parentProjectId);
    @Query("SELECT p FROM Project p JOIN FETCH p.company WHERE p.id = :id")
    Optional<Project> findByIdWithCompany(@Param("id") Long id);
    @Query("""
           select p from Project p
           join fetch p.company c
           where p.status in :statuses and p.reviewer is null
           """)
    Page<Project> findInboxUnassigned(@Param("statuses") Collection<ProjectStatus> statuses, Pageable pageable);

    @Query("""
           select p from Project p
           join fetch p.company c
           where p.status in :statuses and p.reviewer.id = :cvaId
           """)
    Page<Project> findInboxAssigned(@Param("cvaId") Long cvaId,
                                    @Param("statuses") Collection<ProjectStatus> statuses,
                                    Pageable pageable);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    @Query("""
           select p from Project p
           join fetch p.company c
           left join fetch p.reviewer r
           where p.status = com.carbonx.marketcarbon.common.ProjectStatus.CVA_APPROVED
           """)
    Page<Project> findAllCvaApproved(Pageable pageable);

    @Query("""
           select p from Project p
           join fetch p.company c
           left join fetch p.reviewer r
           where p.reviewer.id = :cvaId and p.status in :statuses
           """)
    Page<Project> findReviewedByCva(@Param("cvaId") Long cvaId,
                                    @Param("statuses") Collection<ProjectStatus> statuses,
                                    Pageable pageable);

    // danh sách project gốc (do Admin tạo): company IS NULL + lọc theo trạng thái
    @Query("""
           select p from Project p
           where p.company is null and p.status in :statuses
           """)
    Page<Project> findBaseProjects(@Param("statuses") Collection<ProjectStatus> statuses, Pageable pageable);

    // tìm project gốc theo id
    Optional<Project> findByIdAndCompanyIsNull(Long id);

    Page<Project> findByReviewerAndStatusIn(String reviewer, List<ProjectStatus> statuses, Pageable pageable);

}




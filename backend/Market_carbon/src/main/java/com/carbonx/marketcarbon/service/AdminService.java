package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    ProjectResponse finalApprove(Long projectId, ProjectStatus status);
    Page<ProjectResponse> adminListReviewedByCva(Long cvaId, Pageable pageable);
    Page<ProjectResponse> adminInbox(Pageable pageable);
}

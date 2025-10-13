package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CvaService {
    ProjectResponse review(ProjectReviewRequest request);
    Page<ProjectResponse> cvaInbox(boolean assignedOnly, Pageable pageable);
}

package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.request.ProjectSubmitRequest;
import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.model.Project;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {
     ProjectResponse createProject(ProjectRequest req);
     void updateProject(Long id, ProjectRequest req);
     void deleteProject(Long id);
     List<ProjectDetailResponse> findAllProject();
     ProjectResponse submit(ProjectSubmitRequest request);    // Company nộp hồ sơ
     ProjectResponse sendToReview(Long projectId);            // Chuyển sang UNDER_REVIEW
     ProjectResponse review(ProjectReviewRequest request);    // CVA duyệt/từ chối
     List<ProjectResponse> listAll();
     ProjectResponse getById(Long id);
     ImportReport importCsv(MultipartFile file);
     ProjectResponse finalApprove(Long projectId, String reviewer, ProjectStatus status);


}

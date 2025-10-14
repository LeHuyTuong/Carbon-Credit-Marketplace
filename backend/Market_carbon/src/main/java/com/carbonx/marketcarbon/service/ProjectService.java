package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import java.util.List;

public interface ProjectService {
     ProjectResponse createProject(ProjectRequest req);
     void updateProject(Long id, ProjectRequest req);
     void deleteProject(Long id);
     List<ProjectDetailResponse> findAllProject();
     List<ProjectResponse> listAll();
     ProjectResponse getById(Long id);
}

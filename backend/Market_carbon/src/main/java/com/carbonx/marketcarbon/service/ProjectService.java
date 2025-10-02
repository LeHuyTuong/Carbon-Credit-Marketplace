package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.model.Project;

import java.util.List;

public interface ProjectService {
    public void createProject(ProjectRequest req);
    public void updateProject(Long id, ProjectRequest req);
    public void deleteProject(Long id);
    public List<Project> findProjectById(Long id);

}

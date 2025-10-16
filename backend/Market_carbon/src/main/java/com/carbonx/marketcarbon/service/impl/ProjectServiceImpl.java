package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    public ProjectResponse createProject(ProjectRequest req) {
        if (projectRepository.existsByTitle(req.getTitle())) {
            throw new AppException(ErrorCode.TITTLE_DUPLICATED);
        }

        Project project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .commitments(req.getCommitments())
                .technicalIndicators(req.getTechnicalIndicators())
                .measurementMethod(req.getMeasurementMethod())
                .legalDocsUrl(req.getLegalDocsUrl())
                .status(ProjectStatus.OPEN)
                .build();

        Project saved = projectRepository.save(project);

        return ProjectResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .status(saved.getStatus())
                .commitments(saved.getCommitments())
                .technicalIndicators(saved.getTechnicalIndicators())
                .measurementMethod(saved.getMeasurementMethod())
                .legalDocsUrl(saved.getLegalDocsUrl())
                .build();
    }

    @Override
    public void updateProject(Long id, ProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setCommitments(req.getCommitments());
        project.setTechnicalIndicators(req.getTechnicalIndicators());
        project.setMeasurementMethod(req.getMeasurementMethod());
        project.setLegalDocsUrl(req.getLegalDocsUrl());

        projectRepository.save(project);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(p -> ProjectResponse.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .status(p.getStatus())
                        .commitments(p.getCommitments())
                        .technicalIndicators(p.getTechnicalIndicators())
                        .measurementMethod(p.getMeasurementMethod())
                        .legalDocsUrl(p.getLegalDocsUrl())
                        .build())
                .toList();
    }

    @Override
    public ProjectResponse getById(Long id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        return ProjectResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .status(p.getStatus())
                .commitments(p.getCommitments())
                .technicalIndicators(p.getTechnicalIndicators())
                .measurementMethod(p.getMeasurementMethod())
                .legalDocsUrl(p.getLegalDocsUrl())
                .build();
    }
}

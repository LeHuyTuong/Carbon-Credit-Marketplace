package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.service.ProjectService;
import com.carbonx.marketcarbon.service.S3Service;
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
    private final S3Service s3Service;

    @Override
    public ProjectResponse createProject(ProjectRequest req) {
        if (projectRepository.existsByTitle(req.getTitle())) {
            throw new AppException(ErrorCode.TITTLE_DUPLICATED);
        }

        String logoUrl = null;
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            logoUrl = s3Service.uploadFile(req.getLogo());
        }

        String legalDocsUrl = null;
        if (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty()) {
            legalDocsUrl = s3Service.uploadFile(req.getLegalDocsFile());
        }

        Project project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .commitments(req.getCommitments())
                .technicalIndicators(req.getTechnicalIndicators())
                .measurementMethod(req.getMeasurementMethod())
                .legalDocsFile(legalDocsUrl)
                .logo(logoUrl)
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
                .legalDocsFile(saved.getLegalDocsFile())
                .logo(saved.getLogo())
                .build();
    }

    public void updateProject(Long id, ProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setCommitments(req.getCommitments());
        project.setTechnicalIndicators(req.getTechnicalIndicators());
        project.setMeasurementMethod(req.getMeasurementMethod());

        if (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty()) {
            String legalDocsUrl = s3Service.uploadFile(req.getLegalDocsFile());
            project.setLegalDocsFile(legalDocsUrl);
        }

        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            String logoUrl = s3Service.uploadFile(req.getLogo());
            project.setLogo(logoUrl);
        }

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
                        .legalDocsFile(p.getLegalDocsFile())
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
                .legalDocsFile(p.getLegalDocsFile())
                .build();
    }
}

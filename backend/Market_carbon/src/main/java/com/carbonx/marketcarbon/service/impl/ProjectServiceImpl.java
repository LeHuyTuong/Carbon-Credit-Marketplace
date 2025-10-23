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

        // Upload S3 nếu có
        String logoUrl = null;
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            logoUrl = s3Service.uploadFile(req.getLogo()); // trả về public URL
        }

        String legalDocsUrl = null;
        if (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty()) {
            legalDocsUrl = s3Service.uploadFile(req.getLegalDocsFile()); // trả về public URL
        }

        Project project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .commitments(req.getCommitments())
                .technicalIndicators(req.getTechnicalIndicators())
                .measurementMethod(req.getMeasurementMethod())
                .status(ProjectStatus.OPEN)
                .logo(logoUrl)
                .legalDocsFile(legalDocsUrl)
                .build();

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Override
    public void updateProject(Long id, ProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (req.getStatus() != null) {
            project.setStatus(req.getStatus());
        }
        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setCommitments(req.getCommitments());
        project.setTechnicalIndicators(req.getTechnicalIndicators());
        project.setMeasurementMethod(req.getMeasurementMethod());

        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            String logoUrl = s3Service.uploadFile(req.getLogo());
            project.setLogo(logoUrl);
        }

        if (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty()) {
            String legalDocsUrl = s3Service.uploadFile(req.getLegalDocsFile());
            project.setLegalDocsFile(legalDocsUrl);
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
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProjectResponse getById(Long id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        return toResponse(p); // trả cả logo & legalDocsFile
    }


    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .status(p.getStatus())
                .commitments(p.getCommitments())
                .technicalIndicators(p.getTechnicalIndicators())
                .measurementMethod(p.getMeasurementMethod())
                .logo(p.getLogo())
                .legalDocsFile(p.getLegalDocsFile())
                .build();
    }
}

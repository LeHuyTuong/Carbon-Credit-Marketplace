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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final S3Service s3Service;

    @Override
    public ProjectResponse createProject(ProjectRequest req) {
        final String title = safeTrim(req.getTitle());
        if (projectRepository.existsByTitle(title)) {
            throw new AppException(ErrorCode.TITTLE_DUPLICATED);
        }

        validateDateRange(req.getStartedDate(), req.getEndDate());

        String logoUrl = (req.getLogo() != null && !req.getLogo().isEmpty())
                ? s3Service.uploadFile(req.getLogo())
                : null;

        String legalDocsUrl = (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty())
                ? s3Service.uploadFile(req.getLegalDocsFile())
                : null;

        Project project = Project.builder()
                .title(title)
                .description(req.getDescription())
                .commitments(req.getCommitments())
                .technicalIndicators(req.getTechnicalIndicators())
                .measurementMethod(req.getMeasurementMethod())
                .status(ProjectStatus.OPEN) // có thể chuyển sang req.getStatus() nếu muốn set ngay khi tạo
                .logo(logoUrl)
                .legalDocsFile(legalDocsUrl)
                .startedDate(req.getStartedDate())
                .endDate(req.getEndDate())
                .build();

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Override
    public void updateProject(Long id, ProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // Kiểm tra trùng tiêu đề nếu có đổi
        if (req.getTitle() != null) {
            String newTitle = safeTrim(req.getTitle());
            if (!equalsIgnoreCaseSafe(newTitle, project.getTitle())
                    && projectRepository.existsByTitle(newTitle)) {
                throw new AppException(ErrorCode.TITTLE_DUPLICATED);
            }
            project.setTitle(newTitle);
        }

        // Chuẩn bị giá trị ngày mới để validate
        LocalDate newStart = req.getStartedDate() != null ? req.getStartedDate() : project.getStartedDate();
        LocalDate newEnd   = req.getEndDate() != null ? req.getEndDate() : project.getEndDate();
        validateDateRange(newStart, newEnd);

        // Cập nhật các field mô tả (null-safe: chỉ set nếu có giá trị)
        applyIfNotNull(req.getDescription(), project::setDescription);
        applyIfNotNull(req.getCommitments(), project::setCommitments);
        applyIfNotNull(req.getTechnicalIndicators(), project::setTechnicalIndicators);
        applyIfNotNull(req.getMeasurementMethod(), project::setMeasurementMethod);

        // Trạng thái (nếu cho phép update)
        if (req.getStatus() != null) {
            project.setStatus(req.getStatus());
        }

        // Ngày bắt đầu/kết thúc
        if (req.getStartedDate() != null) project.setStartedDate(req.getStartedDate());
        if (req.getEndDate() != null) project.setEndDate(req.getEndDate());


        // Upload files nếu có
        if (req.getLogo() != null && !req.getLogo().isEmpty()) {
            project.setLogo(s3Service.uploadFile(req.getLogo()));
        }
        if (req.getLegalDocsFile() != null && !req.getLegalDocsFile().isEmpty()) {
            project.setLegalDocsFile(s3Service.uploadFile(req.getLegalDocsFile()));
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
        return toResponse(p);
    }

    // ===== Helpers =====

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE); // nếu chưa có, hãy thêm ErrorCode này
        }
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private <T> void applyIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) setter.accept(value);
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
                .startedDate(p.getStartedDate())
                .endDate(p.getEndDate())
                .build();
    }
}

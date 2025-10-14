package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.request.ProjectSubmitRequest;
import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.request.importing.ProjectCsvRow;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.mapper.ProjectMapper;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        // Kiểm tra trùng tên dự án khung
        if (projectRepository.existsByTitle(req.getTitle())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        // Admin tạo project khung → chưa có company
        Project project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .logo(req.getLogo())
                .status(ProjectStatus.OPEN) // hoặc TEMPLATE
                .commitments(req.getCommitments())
                .technicalIndicators(req.getTechnicalIndicators())
                .measurementMethod(req.getMeasurementMethod())
                .legalDocsUrl(req.getLegalDocsUrl())
                .build();

        projectRepository.save(project);
        log.info(" Project '{}' created by admin {}", project.getTitle(), email);

        return projectMapper.toResponse(project);
    }

    @Override
    public void updateProject(Long id, ProjectRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        Project project = projectRepository.findById(id).
                orElseThrow(()  -> new ResourceNotFoundException("Project not found"));

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setStatus(ProjectStatus.PENDING);
        project.setLogo(req.getLogo());
        projectRepository.save(project);
        log.info("Project updated");
    }

    @Override
    public void deleteProject(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        Project project =  projectRepository.findById(id)
                .orElseThrow(()  -> new ResourceNotFoundException("Project not found"));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectDetailResponse> findAllProject() {
        return projectRepository.findAll().stream()
                .map(project ->  ProjectDetailResponse.builder()
                        .id(project.getId())
                        .title(project.getTitle())
                        .description(project.getDescription())
                        .status(project.getStatus())
                        .logo(project.getLogo())
                        .company(project.getCompany())
                        .updatedAt(project.getUpdatedAt())
                        .createAt(project.getCreateAt())
                        .build()
                )
                .toList();
    }

    @Override
    public ProjectResponse submit(ProjectSubmitRequest request) {
//        Company company = companyRepository.findById(request.getCompanyId())
//                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND)); // đổi SELLER -> COMPANY
//
//        Project project = Project.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .logo(request.getLogo())
//                .status(ProjectStatus.SUBMITTED) // dùng đúng enum của Project
//                .company(company)
//                .commitments(request.getCommitments())
//                .technicalIndicators(request.getTechnicalIndicators())
//                .measurementMethod(request.getMeasurementMethod())
//                .legalDocsUrl(request.getLegalDocsUrl())
//                .build();
//
//        Project saved = projectRepository.save(project);
//        return projectMapper.toResponse(saved);
        return null;
    }

    @Override
    public ProjectResponse sendToReview(Long projectId) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        p.setStatus(ProjectStatus.UNDER_REVIEW);
        Project saved = projectRepository.save(p);
        return projectMapper.toResponse(saved);
    }

    //  Bước 2: CVA thẩm định hồ sơ
    @Override
    public ProjectResponse review(ProjectReviewRequest request) {
        Project p = projectRepository.findByIdWithCompany(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // Chỉ được thẩm định khi đang chờ duyệt
        if (p.getStatus() != ProjectStatus.PENDING_REVIEW && p.getStatus() != ProjectStatus.UNDER_REVIEW) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        p.setReviewer(request.getReviewer());
        p.setReviewNote(request.getReviewNote());

        if (request.getDecision() == ProjectStatus.CVA_APPROVED) {
            p.setStatus(ProjectStatus.CVA_APPROVED);
        } else {
            p.setStatus(ProjectStatus.REJECTED);
        }

        Project saved = projectRepository.save(p);
        return projectMapper.toResponse(saved);
    }

    // 🛠️ Bước 3: Admin xác nhận cuối cùng
    @Override
    public ProjectResponse finalApprove(Long projectId, String reviewer, ProjectStatus status) {
        Project p = projectRepository.findByIdWithCompany(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        //  chỉ duyệt nếu CVA đã phê duyệt
        if (p.getStatus() != ProjectStatus.CVA_APPROVED) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        //  chỉ cho phép ADMIN_APPROVED hoặc REJECTED
        if (status != ProjectStatus.ADMIN_APPROVED && status != ProjectStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_FINAL_APPROVAL_STATUS);
        }

        p.setReviewer(reviewer);
        p.setStatus(status);

        Project saved = projectRepository.save(p);
        return projectMapper.toResponse(saved);
    }





    @Override
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    public ProjectResponse getById(Long id) {
        Project p = projectRepository.findByIdWithCompany(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return projectMapper.toResponse(p);
    }

    @Override
    @SneakyThrows
    public ImportReport importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ImportReport.builder()
                    .total(0).success(0).failed(0)
                    .results(List.of())
                    .build();
        }

        //  Lấy user đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        //  Tìm company của user đăng nhập
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));

        List<ImportReport.RowResult> rowResults = new ArrayList<>();
        int ok = 0, fail = 0;

        CSVFormat fmt = CSVFormat.DEFAULT
                .builder()
                .setHeader("baseProjectId", "title", "description", "logo",
                        "commitments", "technicalIndicators", "measurementMethod", "legalDocsUrl")
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, fmt)) {

            int physicalLine = 1;
            for (CSVRecord r : parser) {
                physicalLine++;
                try {
                    ProjectCsvRow row = mapRecordWithoutCompany(r);
                    validateRow(row);

                    //  Kiểm tra base project (admin tạo)
                    Project baseProject = projectRepository.findById(row.getBaseProjectId())
                            .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

                    //  Tạo project mới cho company
                    Project project = Project.builder()
                            .title(row.getTitle())
                            .description(row.getDescription())
                            .logo(row.getLogo())
                            .status(ProjectStatus.PENDING_REVIEW) // trạng thái mặc định
                            .company(company)
                            .commitments(row.getCommitments())
                            .technicalIndicators(row.getTechnicalIndicators())
                            .measurementMethod(row.getMeasurementMethod())
                            .legalDocsUrl(row.getLegalDocsUrl())
                            .build();

                    Project saved = projectRepository.save(project);

                    rowResults.add(ImportReport.RowResult.builder()
                            .lineNumber(physicalLine)
                            .success(true)
                            .projectId(saved.getId())
                            .titleEcho(saved.getTitle())
                            .error(null)
                            .build());
                    ok++;
                } catch (Exception ex) {
                    log.warn("Import CSV line {} failed: {}", physicalLine, ex.getMessage());
                    rowResults.add(ImportReport.RowResult.builder()
                            .lineNumber(physicalLine)
                            .success(false)
                            .projectId(null)
                            .titleEcho(getSafe(r, "title"))
                            .error(shortError(ex))
                            .build());
                    fail++;
                }
            }
        }

        return ImportReport.builder()
                .total(ok + fail)
                .success(ok)
                .failed(fail)
                .results(rowResults)
                .build();
    }


    private ProjectCsvRow mapRecordWithoutCompany(CSVRecord r) {
        return ProjectCsvRow.builder()
                .baseProjectId(parseLong(getRequired(r, "baseProjectId"), "baseProjectId"))
                .title(getRequired(r, "title"))
                .description(getRequired(r, "description"))
                .logo(getRequired(r, "logo"))
                .commitments(getRequired(r, "commitments"))
                .technicalIndicators(getRequired(r, "technicalIndicators"))
                .measurementMethod(getRequired(r, "measurementMethod"))
                .legalDocsUrl(getOptional(r, "legalDocsUrl"))
                .build();
    }



    private ProjectCsvRow mapRecord(CSVRecord r) {
        return ProjectCsvRow.builder()
                .baseProjectId(parseLong(getRequired(r, "baseProjectId"), "baseProjectId"))
                .companyId(parseLong(getRequired(r, "companyId"), "companyId"))
                .title(getRequired(r, "title"))
                .description(getRequired(r, "description"))
                .logo(getRequired(r, "logo"))
                .commitments(getRequired(r, "commitments"))
                .technicalIndicators(getRequired(r, "technicalIndicators"))
                .measurementMethod(getRequired(r, "measurementMethod"))
                .legalDocsUrl(getOptional(r, "legalDocsUrl"))
                .build();
    }

    private void validateRow(ProjectCsvRow row) {
        if (row.getBaseProjectId() == null || row.getBaseProjectId() <= 0)
            throw new IllegalArgumentException("baseProjectId is required and must be > 0");
        if (row.getTitle() == null || row.getTitle().isBlank())
            throw new IllegalArgumentException("title must not be blank");
    }



    private static String getRequired(CSVRecord r, String key) {
        String v = r.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing column: " + key);
        return v.trim();
    }
    private static String getOptional(CSVRecord r, String key) {
        String v = r.isMapped(key) ? r.get(key) : null;
        return v == null ? null : v.trim();
    }
    private static String getSafe(CSVRecord r, String key) {
        try { return r.get(key); } catch (Exception e) { return null; }
    }
    private static Long parseLong(String s, String field) {
        try { return Long.valueOf(s); } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + field + ": " + s);
        }
    }
    private static String shortError(Exception ex) {
        String msg = ex.getMessage();
        return msg != null && msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }
}


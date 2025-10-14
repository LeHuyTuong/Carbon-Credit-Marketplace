package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.mapper.ProjectMapper;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.ProjectService;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ProjectMapper projectMapper;
    private final CvaRepository cvaRepository;
    private final AdminRepository adminRepository;


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

//    @Override
//    public ProjectResponse submit(ProjectSubmitRequest request) {
////        Company company = companyRepository.findById(request.getCompanyId())
////                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND)); // đổi SELLER -> COMPANY
////
////        Project project = Project.builder()
////                .title(request.getTitle())
////                .description(request.getDescription())
////                .logo(request.getLogo())
////                .status(ProjectStatus.SUBMITTED) // dùng đúng enum của Project
////                .company(company)
////                .commitments(request.getCommitments())
////                .technicalIndicators(request.getTechnicalIndicators())
////                .measurementMethod(request.getMeasurementMethod())
////                .legalDocsUrl(request.getLegalDocsUrl())
////                .build();
////
////        Project saved = projectRepository.save(project);
////        return projectMapper.toResponse(saved);
//        return null;
//    }

//    @Override
//    public ProjectResponse sendToReview(Long projectId) {
//        Project p = projectRepository.findById(projectId)
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//        p.setStatus(ProjectStatus.UNDER_REVIEW);
//        Project saved = projectRepository.save(p);
//        return projectMapper.toResponse(saved);
//    }

    //  Bước 2: CVA thẩm định hồ sơ
//    @Override
//    public ProjectResponse review(ProjectReviewRequest request) {
//        Project p = projectRepository.findByIdWithCompany(request.getProjectId())
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//
//        // Chỉ được thẩm định khi đang chờ duyệt
//        if (p.getStatus() != ProjectStatus.PENDING_REVIEW && p.getStatus() != ProjectStatus.UNDER_REVIEW) {
//            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
//        }
//
//        Cva reviewer = cvaRepository.findById(request.getReviewerId())
//                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));
//
//        p.setReviewer(reviewer);
//        p.setReviewNote(request.getReviewNote());
//
//        if (request.getDecision() == ProjectStatus.CVA_APPROVED) {
//            p.setStatus(ProjectStatus.CVA_APPROVED);
//        } else {
//            p.setStatus(ProjectStatus.REJECTED);
//        }
//
//        Project saved = projectRepository.save(p);
//        log.info(" Project {} reviewed by CVA {}", p.getId(), reviewer.getName());
//        return projectMapper.toResponse(saved);
//    }

    // Bước 3: Admin xác nhận cuối cùng
//    @Override
//    public ProjectResponse finalApprove(Long projectId, ProjectStatus status) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        User current = userRepository.findByEmail(email);
//        if (current == null) {
//            throw new AppException(ErrorCode.USER_NOT_EXISTED);
//        }
//
//        Admin admin = adminRepository.findByUserId(current.getId())
//                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));
//
//        Project p = projectRepository.findByIdWithCompany(projectId)
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//
//        if (p.getStatus() != ProjectStatus.CVA_APPROVED) {
//            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
//        }
//        if (status != ProjectStatus.ADMIN_APPROVED && status != ProjectStatus.REJECTED) {
//            throw new AppException(ErrorCode.INVALID_FINAL_APPROVAL_STATUS);
//        }
//
//        p.setFinalReviewer(admin);
//        p.setFinalReviewNote("Final decision by " + admin.getName());
//        p.setStatus(status);
//
//        Project saved = projectRepository.save(p);
//        log.info(" Project {} final-reviewed by admin {}", saved.getId(), current.getEmail());
//
//        return projectMapper.toResponse(saved);
//    }

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

//    @Override
//    @Transactional(rollbackFor = CsvBatchException.class)
//    @SneakyThrows
//    public ImportReport importCsv(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            return ImportReport.builder()
//                    .total(0).success(0).failed(0)
//                    .results(List.of())
//                    .build();
//        }
//
//        // ======= Lấy user hiện tại =======
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        User user = userRepository.findByEmail(email);
//        if (user == null) {
//            throw new ResourceNotFoundException("User not found");
//        }
//
//        // ======= Tìm company =======
//        Company company = companyRepository.findByUserId(user.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));
//
//        // ======= Chuẩn bị format CSV =======
//        CSVFormat fmt = CSVFormat.DEFAULT.builder()
//                .setHeader("baseProjectId", "title", "description", "logo",
//                        "commitments", "technicalIndicators", "measurementMethod", "legalDocsUrl")
//                .setSkipHeaderRecord(true)
//                .setTrim(true)
//                .build();
//
//        List<ImportReport.RowResult> rowResults = new ArrayList<>();
//        List<ProjectCsvRow> validRows = new ArrayList<>();
//        int line = 1;
//
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
//             CSVParser parser = new CSVParser(reader, fmt)) {
//
//            for (CSVRecord r : parser) {
//                line++;
//                Map<String, String> cols = echoColumns(r);
//                try {
//                    ProjectCsvRow row = mapRecordWithoutCompany(r);
//                    validateRow(row);
//
//                    // Kiểm tra base project tồn tại
//                    if (!projectRepository.existsById(row.getBaseProjectId())) {
//                        throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
//                    }
//
//                    validRows.add(row);
//                    rowResults.add(ImportReport.RowResult.builder()
//                            .lineNumber(line)
//                            .success(true)
//                            .columns(cols)
//                            .build());
//
//                } catch (AppException ex) {
//                    rowResults.add(ImportReport.RowResult.builder()
//                            .lineNumber(line)
//                            .success(false)
//                            .projectId(null)
//                            .columns(cols)
//                            .errorCode(ex.getErrorCode().name())
//                            .errorDetails(ex.getErrorCode().getMessage())
//                            .build());
//                }
//            }
//        }
//
//        // ======= Nếu có bất kỳ lỗi => rollback, trả HTTP 400 =======
//        long failed = rowResults.stream().filter(r -> !r.isSuccess()).count();
//        if (failed > 0) {
//            throw new CsvBatchException(rowResults);
//        }
//
//        // ======= Lưu tất cả dòng hợp lệ =======
//        for (ProjectCsvRow row : validRows) {
//            Project project = Project.builder()
//                    .title(row.getTitle())
//                    .description(row.getDescription())
//                    .logo(row.getLogo())
//                    .status(ProjectStatus.PENDING_REVIEW)
//                    .company(company)
//                    .commitments(row.getCommitments())
//                    .technicalIndicators(row.getTechnicalIndicators())
//                    .measurementMethod(row.getMeasurementMethod())
//                    .legalDocsUrl(row.getLegalDocsUrl())
//                    .build();
//            projectRepository.save(project);
//        }
//
//        return ImportReport.builder()
//                .total(validRows.size())
//                .success(validRows.size())
//                .failed(0)
//                .results(rowResults)
//                .build();
//    }
//
//// ===================== HÀM PHỤ =====================
//
//    private ProjectCsvRow mapRecordWithoutCompany(CSVRecord r) {
//        return ProjectCsvRow.builder()
//                .baseProjectId(parseLong(getRequired(r, "baseProjectId"), "baseProjectId"))
//                .title(getRequired(r, "title"))
//                .description(getRequired(r, "description"))
//                .logo(getRequired(r, "logo"))
//                .commitments(getRequired(r, "commitments"))
//                .technicalIndicators(getRequired(r, "technicalIndicators"))
//                .measurementMethod(getRequired(r, "measurementMethod"))
//                .legalDocsUrl(getRequired(r, "legalDocsUrl"))
//                .build();
//    }
//
//    private void validateRow(ProjectCsvRow row) {
//        if (row.getBaseProjectId() == null || row.getBaseProjectId() <= 0)
//            throw new AppException(ErrorCode.CSV_BASE_PROJECT_ID_INVALID);
//        if (isBlank(row.getTitle()))
//            throw new AppException(ErrorCode.CSV_TITLE_MISSING);
//        if (isBlank(row.getDescription()))
//            throw new AppException(ErrorCode.CSV_DESCRIPTION_MISSING);
//        if (isBlank(row.getLogo()))
//            throw new AppException(ErrorCode.CSV_LOGO_MISSING);
//        if (isBlank(row.getCommitments()))
//            throw new AppException(ErrorCode.CSV_COMMITMENTS_MISSING);
//        if (isBlank(row.getTechnicalIndicators()))
//            throw new AppException(ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING);
//        if (isBlank(row.getMeasurementMethod()))
//            throw new AppException(ErrorCode.CSV_MEASUREMENT_METHOD_MISSING);
//        if (isBlank(row.getLegalDocsUrl()))
//            throw new AppException(ErrorCode.CSV_LEGAL_DOCS_URL_MISSING);
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.trim().isEmpty();
//    }
//
//    private static String getRequired(CSVRecord r, String key) {
//        try {
//            if (!r.isMapped(key)) {
//                throw new AppException(ErrorCode.CSV_MISSING_COLUMN);
//            }
//            String v = r.get(key);
//            if (v == null || v.isBlank()) {
//                throw new AppException(errorCodeForKey(key));
//            }
//            return v.trim();
//        } catch (IllegalArgumentException iae) {
//            throw new AppException(ErrorCode.CSV_MISSING_COLUMN);
//        }
//    }
//
//    private static ErrorCode errorCodeForKey(String key) {
//        switch (key) {
//            case "title":               return ErrorCode.CSV_TITLE_MISSING;
//            case "description":         return ErrorCode.CSV_DESCRIPTION_MISSING;
//            case "logo":                return ErrorCode.CSV_LOGO_MISSING;
//            case "commitments":         return ErrorCode.CSV_COMMITMENTS_MISSING;
//            case "technicalIndicators": return ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING;
//            case "measurementMethod":   return ErrorCode.CSV_MEASUREMENT_METHOD_MISSING;
//            case "legalDocsUrl":        return ErrorCode.CSV_LEGAL_DOCS_URL_MISSING;
//            case "baseProjectId":       return ErrorCode.CSV_BASE_PROJECT_ID_INVALID;
//            default:                    return ErrorCode.CSV_MISSING_FIELD;
//        }
//    }
//
//    private static Long parseLong(String s, String field) {
//        try {
//            return Long.valueOf(s.trim());
//        } catch (NumberFormatException e) {
//            throw new AppException(ErrorCode.CSV_INVALID_NUMBER_FORMAT);
//        }
//    }
//
//    private static String getSafe(CSVRecord r, String key) {
//        try { return r.get(key); } catch (Exception e) { return null; }
//    }
//
//    private static Map<String, String> echoColumns(CSVRecord r) {
//        Map<String, String> m = new LinkedHashMap<>();
//        m.put("baseProjectId", getSafe(r, "baseProjectId"));
//        m.put("title", getSafe(r, "title"));
//        m.put("description", getSafe(r, "description"));
//        m.put("logo", getSafe(r, "logo"));
//        m.put("commitments", getSafe(r, "commitments"));
//        m.put("technicalIndicators", getSafe(r, "technicalIndicators"));
//        m.put("measurementMethod", getSafe(r, "measurementMethod"));
//        m.put("legalDocsUrl", getSafe(r, "legalDocsUrl"));
//        return m;
//    }

    public Page<ProjectResponse> cvaInbox(boolean assignedOnly, Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new AppException(ErrorCode.USER_NOT_EXISTED);

        var cva = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        var statuses = List.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.UNDER_REVIEW);
        Page<Project> page = assignedOnly
                ? projectRepository.findInboxAssigned(cva.getId(), statuses, pageable)
                : projectRepository.findInboxUnassigned(statuses, pageable);

        return page.map(projectMapper::toResponse);
    }

//    public Page<ProjectResponse> adminInbox(Pageable pageable) {
//        return projectRepository.findAllCvaApproved(pageable)
//                .map(projectMapper::toResponse);
//    }

//    public Page<ProjectResponse> adminListReviewedByCva(Long cvaId, Pageable pageable) {
//        var statuses = List.of(ProjectStatus.CVA_APPROVED, ProjectStatus.REJECTED);
//        return projectRepository.findReviewedByCva(cvaId, statuses, pageable)
//                .map(projectMapper::toResponse);
//    }
}


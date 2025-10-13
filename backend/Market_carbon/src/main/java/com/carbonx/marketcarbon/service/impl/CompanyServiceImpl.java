package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.request.importing.ProjectCsvRow;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.CsvBatchException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.mapper.ProjectMapper;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final VehicleRepository vehicleRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final UserRepository userRepository;


    @Override
    public void assignProject(Company company, Project project) {

    }

    @Override
    public void removeProject(Company company, Project project) {

    }

    @Override
    public List<Project> getProjects(Company company) {
        return List.of();
    }

    @Override
    public ProjectResponse sendToReview(Long projectId) {
        // lấy user hiện tại
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new ResourceNotFoundException("User not found");

        // lấy company của user
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));

        // lấy project kèm company để check quyền
        Project p = projectRepository.findByIdWithCompany(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // chỉ cho phép gửi nếu là dự án của chính company
        if (p.getCompany() == null || !p.getCompany().getId().equals(company.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // chỉ chuyển khi đang chờ duyệt
        if (p.getStatus() != ProjectStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        p.setStatus(ProjectStatus.UNDER_REVIEW);
        return projectMapper.toResponse(projectRepository.save(p));
    }


    @Override
    @Transactional(rollbackFor = CsvBatchException.class)
    @SneakyThrows
    public ImportReport importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ImportReport.builder()
                    .total(0).success(0).failed(0)
                    .results(List.of())
                    .build();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));

        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader("baseProjectId", "title", "description", "logo",
                        "commitments", "technicalIndicators", "measurementMethod", "legalDocsUrl")
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        List<ImportReport.RowResult> rowResults = new ArrayList<>();
        List<ProjectCsvRow> validRows = new ArrayList<>();
        int line = 1;

        Set<Long> parentIdsInCsv = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, fmt)) {

            for (CSVRecord r : parser) {
                line++;
                Map<String, String> cols = echoColumns(r);
                try {
                    ProjectCsvRow row = mapRecordWithoutCompany(r);
                    validateRow(row);

                    if (!projectRepository.existsById(row.getBaseProjectId())) {
                        throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
                    }

                    if (!parentIdsInCsv.add(row.getBaseProjectId())) {
                        throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
                    }

                    if (projectRepository.existsByCompanyIdAndParentProjectId(
                            company.getId(), row.getBaseProjectId())) {
                        throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
                    }

                    validRows.add(row);
                    rowResults.add(ImportReport.RowResult.builder()
                            .lineNumber(line)
                            .success(true)
                            .columns(cols)
                            .build());

                } catch (AppException ex) {
                    rowResults.add(ImportReport.RowResult.builder()
                            .lineNumber(line)
                            .success(false)
                            .projectId(null)
                            .columns(cols)
                            .errorCode(ex.getErrorCode().name())
                            .errorDetails(ex.getErrorCode().getMessage())
                            .build());
                }
            }
        }

        long failed = rowResults.stream().filter(r -> !r.isSuccess()).count();
        if (failed > 0) {
            throw new CsvBatchException(rowResults);
        }
        for (ProjectCsvRow row : validRows) {
            Project project = Project.builder()
                    .title(row.getTitle())
                    .description(row.getDescription())
                    .logo(row.getLogo())
                    .status(ProjectStatus.PENDING_REVIEW)
                    .company(company)
                    .commitments(row.getCommitments())
                    .technicalIndicators(row.getTechnicalIndicators())
                    .measurementMethod(row.getMeasurementMethod())
                    .legalDocsUrl(row.getLegalDocsUrl())
                    .build();
            projectRepository.save(project);
        }

        return ImportReport.builder()
                .total(validRows.size())
                .success(validRows.size())
                .failed(0)
                .results(rowResults)
                .build();
    }

    @Override
    @Transactional
    public ProjectResponse applyToBaseProject(Long baseProjectId) {
        // 1) user & company hiện tại
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new ResourceNotFoundException("User not found");
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));

        // 2) lấy project gốc
        Project base = projectRepository.findByIdAndCompanyIsNull(baseProjectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        //  1 công ty / 1 base project
        if (projectRepository.existsByCompanyIdAndParentProjectId(company.getId(), base.getId())) {
            throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
        }

        // 4) tạo hồ sơ NHÁP cho công ty (company có thể sửa các field sau đó)
        Project draft = Project.builder()
                .title(base.getTitle())
                .description(base.getDescription())
                .logo(base.getLogo())
                .status(ProjectStatus.DRAFT)
                .company(company)
                .parentProjectId(base.getId())
                .commitments(base.getCommitments())
                .technicalIndicators(base.getTechnicalIndicators())
                .measurementMethod(base.getMeasurementMethod())
                .legalDocsUrl(base.getLegalDocsUrl())
                .build();

        Project saved = projectRepository.save(draft);
        return projectMapper.toResponse(saved);
    }

    @Override
    public Page<ProjectResponse> listBaseProjectChoices(Pageable pageable) {
        var statuses = List.of(ProjectStatus.OPEN, ProjectStatus.PENDING);
        Page<Project> page = projectRepository.findBaseProjects(statuses, pageable);
        return page.map(projectMapper::toResponse);
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
                .legalDocsUrl(getRequired(r, "legalDocsUrl"))
                .build();
    }

    private void validateRow(ProjectCsvRow row) {
        if (row.getBaseProjectId() == null || row.getBaseProjectId() <= 0)
            throw new AppException(ErrorCode.CSV_BASE_PROJECT_ID_INVALID);
        if (isBlank(row.getTitle()))
            throw new AppException(ErrorCode.CSV_TITLE_MISSING);
        if (isBlank(row.getDescription()))
            throw new AppException(ErrorCode.CSV_DESCRIPTION_MISSING);
        if (isBlank(row.getLogo()))
            throw new AppException(ErrorCode.CSV_LOGO_MISSING);
        if (isBlank(row.getCommitments()))
            throw new AppException(ErrorCode.CSV_COMMITMENTS_MISSING);
        if (isBlank(row.getTechnicalIndicators()))
            throw new AppException(ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING);
        if (isBlank(row.getMeasurementMethod()))
            throw new AppException(ErrorCode.CSV_MEASUREMENT_METHOD_MISSING);
        if (isBlank(row.getLegalDocsUrl()))
            throw new AppException(ErrorCode.CSV_LEGAL_DOCS_URL_MISSING);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String getRequired(CSVRecord r, String key) {
        try {
            if (!r.isMapped(key)) {
                throw new AppException(ErrorCode.CSV_MISSING_COLUMN);
            }
            String v = r.get(key);
            if (v == null || v.isBlank()) {
                throw new AppException(errorCodeForKey(key));
            }
            return v.trim();
        } catch (IllegalArgumentException iae) {
            throw new AppException(ErrorCode.CSV_MISSING_COLUMN);
        }
    }

    private static ErrorCode errorCodeForKey(String key) {
        switch (key) {
            case "title":               return ErrorCode.CSV_TITLE_MISSING;
            case "description":         return ErrorCode.CSV_DESCRIPTION_MISSING;
            case "logo":                return ErrorCode.CSV_LOGO_MISSING;
            case "commitments":         return ErrorCode.CSV_COMMITMENTS_MISSING;
            case "technicalIndicators": return ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING;
            case "measurementMethod":   return ErrorCode.CSV_MEASUREMENT_METHOD_MISSING;
            case "legalDocsUrl":        return ErrorCode.CSV_LEGAL_DOCS_URL_MISSING;
            case "baseProjectId":       return ErrorCode.CSV_BASE_PROJECT_ID_INVALID;
            default:                    return ErrorCode.CSV_MISSING_FIELD;
        }
    }

    private static Long parseLong(String s, String field) {
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.CSV_INVALID_NUMBER_FORMAT);
        }
    }

    private static String getSafe(CSVRecord r, String key) {
        try { return r.get(key); } catch (Exception e) { return null; }
    }

    private static Map<String, String> echoColumns(CSVRecord r) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("baseProjectId", getSafe(r, "baseProjectId"));
        m.put("title", getSafe(r, "title"));
        m.put("description", getSafe(r, "description"));
        m.put("logo", getSafe(r, "logo"));
        m.put("commitments", getSafe(r, "commitments"));
        m.put("technicalIndicators", getSafe(r, "technicalIndicators"));
        m.put("measurementMethod", getSafe(r, "measurementMethod"));
        m.put("legalDocsUrl", getSafe(r, "legalDocsUrl"));
        return m;
    }

    @Override
    public void createCompany(Company company) {

    }

    @Override
    public void updateCompany(Long id, Company company) {

    }

    @Override
    public void deleteCompany(Long id) {

    }

    @Override
    public void getCompanyById(Long id) {

    }

    @Override
    public List<Company> getAllCompanies() {
        return List.of();
    }
}

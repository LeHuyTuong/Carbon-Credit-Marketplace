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
        // business logic nếu cần
    }

    @Override
    public void removeProject(Company company, Project project) {
        // business logic nếu cần
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

        // 1) Lấy user & company hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));

        // 2) Định nghĩa header CSV: CHỈ gồm field DN phải nộp (khớp ProjectSubmitRequest)
        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "baseProjectId",
                        "companyCommitment",
                        "technicalIndicators",
                        "measurementMethod"
                )
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        List<ImportReport.RowResult> rowResults = new ArrayList<>();
        List<ProjectCsvRow> validRows = new ArrayList<>();
        int line = 1;

        // chặn trùng baseProjectId trong chính file CSV
        Set<Long> baseIdsInCsv = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, fmt)) {

            for (CSVRecord r : parser) {
                line++;
                Map<String, String> cols = echoColumnsBatchSubmit(r);
                try {
                    ProjectCsvRow row = mapRecordForBatchSubmit(r);
                    validateBatchSubmitRow(row);

                    // 3) Base project phải là project gốc (company = null)
                    Project base = projectRepository.findByIdAndCompanyIsNull(row.getBaseProjectId())
                            .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

                    // 4) Chặn trùng trong CSV
                    if (!baseIdsInCsv.add(row.getBaseProjectId())) {
                        throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
                    }

                    // 5) Chặn nếu công ty đã nộp trước đó cho base này
                    if (projectRepository.existsByCompanyIdAndParentProjectId(company.getId(), base.getId())) {
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

        // Nếu có bất kỳ dòng lỗi => rollback toàn bộ batch
        long failed = rowResults.stream().filter(r -> !r.isSuccess()).count();
        if (failed > 0) {
            throw new CsvBatchException(rowResults);
        }

        // 6) Lưu tất cả dòng hợp lệ: copy meta từ base, gắn parent, ghi dữ liệu DN nộp
        for (ProjectCsvRow row : validRows) {
            Project base = projectRepository.findByIdAndCompanyIsNull(row.getBaseProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

            Project submission = Project.builder()
                    // meta từ base project do Admin tạo
                    .title(base.getTitle())
                    .description(base.getDescription())
                    .logo(base.getLogo())
                    // trạng thái & liên kết
                    .status(ProjectStatus.PENDING_REVIEW)
                    .company(company)
                    .parentProjectId(base.getId())
                    // dữ liệu DN nộp
                    .commitments(row.getCompanyCommitment())
                    .technicalIndicators(row.getTechnicalIndicators())
                    .measurementMethod(row.getMeasurementMethod())
                    .build();

            projectRepository.save(submission);
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

        // 2) lấy project gốc (Admin)
        Project base = projectRepository.findByIdAndCompanyIsNull(baseProjectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // 3) 1 company / 1 base project
        if (projectRepository.existsByCompanyIdAndParentProjectId(company.getId(), base.getId())) {
            throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
        }

        // 4) tạo hồ sơ NHÁP cho công ty (company sẽ bổ sung & submit sau)
        Project draft = Project.builder()
                .title(base.getTitle())
                .description(base.getDescription())
                .logo(base.getLogo())
                .status(ProjectStatus.DRAFT)
                .company(company)
                .parentProjectId(base.getId())
                .commitments(null)
                .technicalIndicators(null)
                .measurementMethod(null)
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


    private ProjectCsvRow mapRecordForBatchSubmit(CSVRecord r) {
        return ProjectCsvRow.builder()
                .baseProjectId(parseLong(getRequired(r, "baseProjectId"), "baseProjectId"))
                .companyCommitment(getRequired(r, "companyCommitment"))
                .technicalIndicators(getRequired(r, "technicalIndicators"))
                .measurementMethod(getRequired(r, "measurementMethod"))
                .build();
    }

    private void validateBatchSubmitRow(ProjectCsvRow row) {
        if (row.getBaseProjectId() == null || row.getBaseProjectId() <= 0)
            throw new AppException(ErrorCode.CSV_BASE_PROJECT_ID_INVALID);
        if (isBlank(row.getCompanyCommitment()))
            throw new AppException(ErrorCode.CSV_COMMITMENTS_MISSING);
        if (isBlank(row.getTechnicalIndicators()))
            throw new AppException(ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING);
        if (isBlank(row.getMeasurementMethod()))
            throw new AppException(ErrorCode.CSV_MEASUREMENT_METHOD_MISSING);
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
            case "companyCommitment":   return ErrorCode.CSV_COMMITMENTS_MISSING;
            case "technicalIndicators": return ErrorCode.CSV_TECHNICAL_INDICATORS_MISSING;
            case "measurementMethod":   return ErrorCode.CSV_MEASUREMENT_METHOD_MISSING;
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

    private static Map<String, String> echoColumnsBatchSubmit(CSVRecord r) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("baseProjectId", getSafe(r, "baseProjectId"));
        m.put("companyCommitment", getSafe(r, "companyCommitment"));
        m.put("technicalIndicators", getSafe(r, "technicalIndicators"));
        m.put("measurementMethod", getSafe(r, "measurementMethod"));
        return m;
    }
}

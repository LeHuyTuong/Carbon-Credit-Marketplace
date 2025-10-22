package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.dto.request.ProjectApplicationRequest;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.FileStorageService;
import com.carbonx.marketcarbon.service.ProjectApplicationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private final ProjectApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final CvaRepository cvaRepository;
    private final AdminRepository adminRepository;
    private  final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public ProjectApplicationResponse submit(Long projectId, MultipartFile file) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.COMPANY_NOT_FOUND);

        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        boolean exists = applicationRepository.existsByCompanyAndProject(company, project);
        if (exists) {
            throw new AppException(ErrorCode.APPLICATION_EXISTED);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "application.pdf";
        String folder = String.format("applications/company-%d/project-%d", company.getId(), project.getId());
        FileStorageService.PutResult put = fileStorageService.putObject(folder, originalName, file);

        ProjectApplication app = ProjectApplication.builder()
                .project(project)
                .company(company)
                .applicationDocsPath(put.key())
                .applicationDocsUrl(put.url())
                .status(ApplicationStatus.UNDER_REVIEW)
                .submittedAt(OffsetDateTime.now())
                .build();

        applicationRepository.save(app);
        return ProjectApplicationResponse.fromEntity(app);
    }

    @Override
    public ProjectApplicationResponse cvaDecision(Long applicationId, boolean approved, String note) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.CVA_NOT_FOUND);

        Cva reviewer = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        ProjectApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW && app.getStatus() != ApplicationStatus.NEEDS_REVISION) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        app.setReviewer(reviewer);
        app.setReviewNote(note);
        app.setStatus(approved ? ApplicationStatus.CVA_APPROVED : ApplicationStatus.CVA_REJECTED);

        ProjectApplication saved = applicationRepository.save(app);
        return ProjectApplicationResponse.fromEntity(saved);
    }


    @Override
    public ProjectApplicationResponse adminFinalDecision(Long applicationId, boolean approved, String note) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.ADMIN_NOT_FOUND);

        Admin admin = adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        ProjectApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        app.setFinalReviewer(admin);
        app.setFinalReviewNote(note);
        app.setStatus(approved ? ApplicationStatus.ADMIN_APPROVED : ApplicationStatus.ADMIN_REJECTED);

        ProjectApplication saved = applicationRepository.save(app);
        return ProjectApplicationResponse.fromEntity(saved);
    }

    @Override
    public List<ProjectApplicationResponse> listMyApplications(String status) {
        // Lấy user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email = auth.getName();

        // Query theo email (tránh phải truyền companyId)
        List<ProjectApplication> apps;
        if (status == null || status.isBlank()) {
            apps = applicationRepository.findByCompany_User_EmailOrderBySubmittedAtDesc(email);
        } else {
            final ApplicationStatus parsed;
            try {
                parsed = ApplicationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_STATUS);
            }
            apps = applicationRepository.findByCompany_User_EmailAndStatusOrderBySubmittedAtDesc(email, parsed);
        }

        return apps.stream().map(this::toResponse).toList();
    }

    @Override
    public List<ProjectApplicationResponse> listAll() {
        return applicationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public ProjectApplicationResponse getById(Long id) {
        ProjectApplication a = applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
        return toResponse(a);
    }

    @Override
    public Page<ProjectApplicationResponse> listCvaApprovedApplications(Pageable pageable) {
        Page<ProjectApplication> apps = applicationRepository.findByStatus(ApplicationStatus.CVA_APPROVED, pageable);
        return apps.map(this::toResponse);
    }

    @Override
    public List<ProjectApplicationResponse> listPendingForCva() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.CVA_NOT_FOUND);

        // kiểm tra CVA hợp lệ
        Cva reviewer = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        // Lấy tất cả hồ sơ đang chờ CVA duyệt
        List<ProjectApplication> pending = applicationRepository.findByStatusOrderBySubmittedAtDesc(ApplicationStatus.UNDER_REVIEW);

        return pending.stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectApplicationResponse toResponse(ProjectApplication a) {
        return ProjectApplicationResponse.builder()
                .id(a.getId())
                .projectId(a.getProject().getId())
                .projectTitle(a.getProject().getTitle())
                .companyId(a.getCompany().getId())
                .companyName(a.getCompany().getCompanyName())
                .status(a.getStatus())
                .reviewNote(a.getReviewNote())
                .finalReviewNote(a.getFinalReviewNote())
                .applicationDocsUrl(a.getApplicationDocsUrl())
                .submittedAt(a.getSubmittedAt())
                .build();
    }
}
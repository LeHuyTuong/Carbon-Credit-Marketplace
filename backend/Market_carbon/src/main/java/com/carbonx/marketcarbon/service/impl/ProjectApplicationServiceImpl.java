package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.EmailService;
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

import java.time.LocalDateTime;
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
    private final EmailService emailService;
    private final ApplicationNotificationService notificationService;

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

        boolean exists = applicationRepository.existsByCompanyAndProjectAndStatusIn(company, project,
                List.of( ApplicationStatus.SUBMITTED,
                        ApplicationStatus.UNDER_REVIEW,
                        ApplicationStatus.CVA_APPROVED,
                        ApplicationStatus.NEEDS_REVISION)
                );
        if (exists) {
            throw new AppException(ErrorCode.APPLICATION_PROCESSING);
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
                .submittedAt(LocalDateTime.now())
                .build();

        applicationRepository.save(app);
        return ProjectApplicationResponse.fromEntity(app);
    }

    @Override
    @Transactional
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
        app.setReviewedAt(LocalDateTime.now());

        ProjectApplication saved = applicationRepository.save(app);

        Company company = app.getCompany();
        Project project = app.getProject();
        String reviewerName = reviewer.getName(); // hoặc getDisplayName()

        notificationService.sendCvaDecision(
                company.getUser().getEmail(),
                company.getCompanyName(),
                app.getId(),
                project.getTitle(),
                reviewerName,
                approved,
                note
        );

        return ProjectApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
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

        Company company = app.getCompany();
        Project project = app.getProject();
        String reviewerName = admin.getName();

        notificationService.sendAdminDecision(
                company.getUser().getEmail(),
                company.getCompanyName(),
                app.getId(),
                project.getTitle(),
                reviewerName,
                approved,
                note
        );

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
        List<ApplicationStatus> visibleStatuses = List.of(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.CVA_APPROVED,
                ApplicationStatus.CVA_REJECTED
        );

        List<ProjectApplication> pending = applicationRepository
                .findByStatusInOrderBySubmittedAtDesc(visibleStatuses);


        return pending.stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectApplicationResponse toResponse(ProjectApplication a) {
        String cvaName = null;
        String adminName = null;
        String waitingFor = null;

        if (a.getReviewer() != null) {
            cvaName = a.getReviewer().getDisplayName();
        }
        if (a.getFinalReviewer() != null) {
            adminName = a.getFinalReviewer().getDisplayName();
        }

        // xác định “đang chờ ai duyệt”
        switch (a.getStatus()) {
            case UNDER_REVIEW ->
                    waitingFor = "Waiting for CVA review — please wait until the CVA completes the evaluation.";
            case CVA_APPROVED ->
                    waitingFor = "Waiting for Admin approval — your application has passed the CVA review and is now pending final approval from the Admin.";
            case CVA_REJECTED ->
                    waitingFor = "Rejected by CVA — please review the CVA’s feedback, make corrections, and resubmit your application.";
            case ADMIN_APPROVED ->
                    waitingFor = "Approved by Admin — your application is complete. You can now join the project and upload emission reports for credit issuance.";
            case ADMIN_REJECTED ->
                    waitingFor = "Rejected by Admin — please review the Admin’s feedback, update your documents or data, and resubmit if applicable.";
            case NEEDS_REVISION ->
                    waitingFor = "Requires revision and resubmission — please update the required sections and upload the revised documents.";
            default ->
                    waitingFor = "Unknown status — please contact system support or the CVA team for clarification.";
        }

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
                .applicationDocsPath(a.getApplicationDocsPath())
                .submittedAt(a.getSubmittedAt())
                .cvaReviewerName(cvaName)
                .adminReviewerName(adminName)
                .waitingFor(waitingFor)
                .build();
    }

}
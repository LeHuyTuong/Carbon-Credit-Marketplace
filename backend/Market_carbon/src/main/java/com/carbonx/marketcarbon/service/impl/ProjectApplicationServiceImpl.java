package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.dto.request.ProjectApplicationRequest;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.ProjectApplicationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    @Override
    public ProjectApplicationResponse submit(ProjectApplicationRequest req) {
        // Lấy user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName(); // chính là email trong JWT token

        //  Tìm user tương ứng với email
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.COMPANY_NOT_FOUND);

        // Lấy company tương ứng với user (mỗi user company có 1 record)
        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        // Kiểm tra project tồn tại
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        //  Kiểm tra xem công ty này đã nộp hồ sơ cho project này chưa
        boolean exists = applicationRepository.existsByCompanyAndProject(company, project);
        if (exists) {
            throw new AppException(ErrorCode.APPLICATION_EXISTED);
        }

        //
        ProjectApplication app = ProjectApplication.builder()
                .project(project)
                .company(company)
                .applicationDocsUrl(req.getApplicationDocsUrl())
                .status(ApplicationStatus.UNDER_REVIEW) // gửi cho CVA duyệt
                .submittedAt(OffsetDateTime.now())
                .build();

        //
        applicationRepository.save(app);

        //
        return ProjectApplicationResponse.fromEntity(app);
    }



    @Override
    public ProjectApplicationResponse cvaDecision(Long applicationId, boolean approved, String note) {
        // 1️⃣ Lấy user hiện tại từ token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.CVA_NOT_FOUND);

        // 2️⃣ Tìm CVA tương ứng với user
        Cva reviewer = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        // 3️⃣ Tìm đơn
        ProjectApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        // 4️⃣ Kiểm tra trạng thái hiện tại
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW && app.getStatus() != ApplicationStatus.NEEDS_REVISION) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 5️⃣ Cập nhật kết quả duyệt
        app.setReviewer(reviewer);
        app.setReviewNote(note);
        app.setStatus(approved ? ApplicationStatus.CVA_APPROVED : ApplicationStatus.CVA_REJECTED);

        ProjectApplication saved = applicationRepository.save(app);
        return ProjectApplicationResponse.fromEntity(saved);
    }


    @Override
    public ProjectApplicationResponse adminFinalDecision(Long applicationId, boolean approved, String note) {
        // 1️⃣ Lấy user hiện tại từ token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.ADMIN_NOT_FOUND);

        // 2️⃣ Tìm Admin tương ứng với user đó
        Admin admin = adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        // 3️⃣ Tìm hồ sơ
        ProjectApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        // 4️⃣ Chỉ cho phép duyệt nếu đã được CVA phê duyệt trước đó
        if (app.getStatus() != ApplicationStatus.CVA_APPROVED) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 5️⃣ Cập nhật kết quả duyệt
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
            throw new AppException(ErrorCode.UNAUTHORIZED); // đổi ErrorCode cho khớp dự án bạn
        }
        String email = auth.getName(); // mặc định Spring Security đặt username = email

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
                .build();
    }
}
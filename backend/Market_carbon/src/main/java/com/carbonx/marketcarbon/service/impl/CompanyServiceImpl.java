//package com.carbonx.marketcarbon.service.impl;
//
//import com.carbonx.marketcarbon.common.KycStatus;
//import com.carbonx.marketcarbon.common.ProjectStatus;
//import com.carbonx.marketcarbon.dto.request.ProjectRegisterRequest;
//import com.carbonx.marketcarbon.dto.response.ProjectResponse;
//import com.carbonx.marketcarbon.exception.AppException;
//import com.carbonx.marketcarbon.exception.ErrorCode;
//import com.carbonx.marketcarbon.exception.ResourceNotFoundException;

//import com.carbonx.marketcarbon.model.Company;
//import com.carbonx.marketcarbon.model.Project;
//import com.carbonx.marketcarbon.model.User;
//import com.carbonx.marketcarbon.repository.CompanyRepository;
//import com.carbonx.marketcarbon.repository.KycRepository;
//import com.carbonx.marketcarbon.repository.ProjectRepository;
//import com.carbonx.marketcarbon.repository.UserRepository;
//import com.carbonx.marketcarbon.service.CompanyService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CompanyServiceImpl implements CompanyService {
//
//    private final CompanyRepository companyRepository;
//    private final ProjectRepository projectRepository;
//    private final ProjectMapper projectMapper;
//    private final UserRepository userRepository;
//    private final KycRepository kycRepository;
//
//
//    @Override
//    public List<Project> getProjects(Company company) {
//        // Tuỳ repo của bạn, có thể đổi sang projectRepository.findAllByCompanyId(company.getId()).
//        return List.of();
//    }
//
//    /** Luồng mới: đăng ký & gửi duyệt trong một bước, không upload file trực tiếp. */
//    @Override
//    @Transactional
//    public ProjectResponse registerAndSubmit(ProjectRegisterRequest req) {
//        // 1) User hiện tại
//        var auth = SecurityContextHolder.getContext().getAuthentication();
//        User user = userRepository.findByEmail(auth.getName());
//        if (user == null) throw new ResourceNotFoundException("User not found");
//
//        //  Company bắt buộc đã tồn tại (tiền điều kiện: user đã KYC company)
//        Company company = companyRepository.findByUserId(user.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("Company not found for user"));
//
//        //  Base project (Admin tạo: company = null)
//        Project base = projectRepository.findByIdAndCompanyIsNull(req.getBaseProjectId())
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//
//        //  Chặn nộp trùng: 1 company / 1 base project
//        if (projectRepository.existsByCompanyIdAndParentProjectId(company.getId(), base.getId())) {
//            throw new AppException(ErrorCode.ONE_APPLICATION_PER_PROJECT);
//        }
//
//        //  Tạo Project con và chuyển thẳng UNDER_REVIEW (gộp đăng ký + gửi duyệt)
//        Project submission = Project.builder()
//                .title(base.getTitle())
//                .description(base.getDescription())
//                .logo(base.getLogo())
//                .status(ProjectStatus.UNDER_REVIEW)
//                .company(company)
//                .parentProjectId(base.getId())
//                .legalDocsUrl(req.getDocument()) // URL hoặc key S3; nếu bắt buộc có tài liệu, hãy validate notBlank
//                .build();
//
//        Project saved = projectRepository.save(submission);
//        return projectMapper.toResponse(saved);
//    }
//
//
//    @Override
//    public Page<ProjectResponse> listBaseProjectChoices(Pageable pageable) {
//        var statuses = List.of(ProjectStatus.OPEN, ProjectStatus.PENDING);
//        Page<Project> page = projectRepository.findBaseProjects(statuses, pageable);
//        return page.map(projectMapper::toResponse);
//    }
//
//    // ===== helpers =====
//    private static boolean isBlank(String s) {
//        return s == null || s.trim().isEmpty(); }
//    private static String firstNonBlank(String a, String b) {
//        return !isBlank(a) ? a.trim() : b; }
//}

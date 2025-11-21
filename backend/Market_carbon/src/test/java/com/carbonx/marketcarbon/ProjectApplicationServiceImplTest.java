package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.FileStorageService;
import com.carbonx.marketcarbon.service.impl.ProjectApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho Luồng 1: Đăng ký và Thẩm định Dự án.
 */
@ExtendWith(MockitoExtension.class)
class ProjectApplicationServiceImplTest {

    @Mock private ProjectApplicationRepository applicationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CvaRepository cvaRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ApplicationNotificationService notificationService;

    // Mocks cho SecurityContext
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProjectApplicationServiceImpl projectApplicationService;

    private User companyUser, cvaUser, adminUser;
    private Company company;
    private Project project;
    private ProjectApplication application;
    private Cva cva;
    private Admin admin;

    @BeforeEach
    void setUp() {
        // Thiết lập User và Role
        companyUser = User.builder().id(1L).email("company@test.com").build();
        cvaUser = User.builder().id(2L).email("cva@test.com").build();
        adminUser = User.builder().id(3L).email("admin@test.com").build();

        company = Company.builder().id(10L).user(companyUser).companyName("Test Company").build();
        project = Project.builder().id(100L).title("EV Project").build();
        cva = Cva.builder().id(20L).user(cvaUser).name("Test CVA").build();
        admin = Admin.builder().id(30L).user(adminUser).name("Test Admin").build();

        application = ProjectApplication.builder()
                .id(1L)
                .project(project)
                .company(company)
                .status(ApplicationStatus.UNDER_REVIEW)
                .submittedAt(LocalDateTime.now())
                .build();

        // Mock SecurityContextHolder mặc định
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // Helper thiết lập mock cho user đang đăng nhập
    private void mockSecurityContext(User user) {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.isAuthenticated()).thenReturn(true);
        // "anonymousUser" check
        lenient().when(authentication.getPrincipal()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
    }

    @Test
    @DisplayName("[Luồng 1 - Submit] Đăng ký dự án thành công")
    void submit_Success() {
        // Arrange
        mockSecurityContext(companyUser);
        when(companyRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(applicationRepository.existsByCompanyAndProjectAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(fileStorageService.putObject(anyString(), anyString(), any(MultipartFile.class)))
                .thenReturn(new FileStorageService.PutResult("path/key", "http://url.com/key"));

        // Sửa lỗi NPE: Mock behavior của save
        when(applicationRepository.save(any(ProjectApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        ArgumentCaptor<ProjectApplication> captor = ArgumentCaptor.forClass(ProjectApplication.class);

        // Act
        ProjectApplicationResponse response = projectApplicationService.submit(project.getId(), file);

        // Assert
        verify(applicationRepository).save(captor.capture());
        ProjectApplication savedApp = captor.getValue();

        assertThat(response).isNotNull();
        assertThat(savedApp.getCompany()).isEqualTo(company);
        assertThat(savedApp.getProject()).isEqualTo(project);
        assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(savedApp.getApplicationDocsUrl()).isEqualTo("http://url.com/key");
    }

    @Test
    @DisplayName("[Luồng 1 - Submit] Thất bại khi dự án đã được đăng ký (đang xử lý)")
    void submit_Fail_WhenApplicationProcessing() {
        // Arrange
        mockSecurityContext(companyUser);
        when(companyRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(applicationRepository.existsByCompanyAndProjectAndStatusIn(any(), any(), anyList())).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> projectApplicationService.submit(project.getId(), file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("[Luồng 1 - CVA Review] CVA duyệt đơn thành công")
    void cvaDecision_Success_Approve() {
        // Arrange
        mockSecurityContext(cvaUser);
        when(cvaRepository.findByUserId(cvaUser.getId())).thenReturn(Optional.of(cva));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application)); // Status là UNDER_REVIEW

        // Sửa lỗi NPE: Mock behavior của save
        when(applicationRepository.save(any(ProjectApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(notificationService).sendCvaDecision(anyString(), anyString(), anyLong(), anyString(), anyString(), eq(true), anyString());
        ArgumentCaptor<ProjectApplication> captor = ArgumentCaptor.forClass(ProjectApplication.class);

        // Act
        projectApplicationService.cvaDecision(application.getId(), true, "CVA Approved");

        // Assert
        verify(applicationRepository).save(captor.capture());
        ProjectApplication savedApp = captor.getValue();

        assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatus.CVA_APPROVED);
        assertThat(savedApp.getReviewer()).isEqualTo(cva);
        assertThat(savedApp.getReviewNote()).isEqualTo("CVA Approved");
        verify(notificationService).sendCvaDecision(any(), any(), any(), any(), any(), eq(true), any());
    }

    @Test
    @DisplayName("[Luồng 1 - CVA Review] Thất bại khi duyệt đơn đã được xử lý (Invalid Status)")
    void cvaDecision_Fail_InvalidStatusTransition() {
        // Arrange
        application.setStatus(ApplicationStatus.ADMIN_APPROVED); // Đơn đã được Admin duyệt
        mockSecurityContext(cvaUser);
        when(cvaRepository.findByUserId(cvaUser.getId())).thenReturn(Optional.of(cva));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        // Act & Assert
        assertThatThrownBy(() -> projectApplicationService.cvaDecision(application.getId(), true, "Late approval"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("[Luồng 1 - Admin Review] Admin duyệt đơn thành công")
    void adminFinalDecision_Success_Approve() {
        // Arrange
        application.setStatus(ApplicationStatus.CVA_APPROVED); // Điều kiện: Phải được CVA duyệt trước
        mockSecurityContext(adminUser);
        when(adminRepository.findByUserId(adminUser.getId())).thenReturn(Optional.of(admin));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        // Sửa lỗi NPE: Mock behavior của save
        when(applicationRepository.save(any(ProjectApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(notificationService).sendAdminDecision(anyString(), anyString(), anyLong(), anyString(), anyString(), eq(true), anyString());
        ArgumentCaptor<ProjectApplication> captor = ArgumentCaptor.forClass(ProjectApplication.class);

        // Act
        projectApplicationService.adminFinalDecision(application.getId(), true, "Admin Approved");

        // Assert
        verify(applicationRepository).save(captor.capture());
        ProjectApplication savedApp = captor.getValue();

        assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatus.ADMIN_APPROVED);
        assertThat(savedApp.getFinalReviewer()).isEqualTo(admin);
        assertThat(savedApp.getFinalReviewNote()).isEqualTo("Admin Approved");
        verify(notificationService).sendAdminDecision(any(), any(), any(), any(), any(), eq(true), any());
    }
}

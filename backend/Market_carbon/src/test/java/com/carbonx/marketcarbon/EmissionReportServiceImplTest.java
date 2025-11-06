package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.helper.notification.ReportNotificationService;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.AiScoringService;
import com.carbonx.marketcarbon.service.FileStorageService;
import com.carbonx.marketcarbon.service.impl.EmissionReportServiceImpl;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho Luồng 2: Thẩm định Báo cáo.
 */
@ExtendWith(MockitoExtension.class)
class EmissionReportServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private EmissionReportRepository reportRepository;
    @Mock private EmissionReportDetailRepository detailRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService storage;
    @Mock private AiScoringService aiScoringService;
    @Mock private CvaRepository cvaRepository;
    @Mock private ReportNotificationService notificationService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private EmissionReportServiceImpl emissionReportService;

    private User companyUser, cvaUser, adminUser;
    private Company company;
    private Project project;
    private Cva cva;
    private EmissionReport submittedReport;

    @BeforeEach
    void setUp() {
        companyUser = User.builder().id(1L).email("company@test.com").build();
        company = Company.builder().id(10L).user(companyUser).companyName("Test Company").build();
        project = Project.builder().id(100L).title("EV Project").build();

        cvaUser = User.builder().id(2L).email("cva@test.com").build();
        cva = Cva.builder().id(20L).user(cvaUser).name("Test CVA").organization("CVA Org").build();

        adminUser = User.builder().id(3L).email("admin@test.com").build();

        submittedReport = EmissionReport.builder()
                .id(1L)
                .seller(company)
                .project(project)
                .status(EmissionStatus.SUBMITTED) // Trạng thái chờ CVA
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockSecurityContext(User user) {
        when(authentication.getName()).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
    }

    @Test
    @DisplayName("[Luồng 2 - Upload] Upload CSV thành công")
    void uploadCsvAsReport_Success() {
        // Arrange
        mockSecurityContext(companyUser);
        when(companyRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(storage.putObject(anyString(), anyString(), any())).thenReturn(new FileStorageService.PutResult("key", "url"));
        when(reportRepository.findBySellerIdAndProjectIdAndPeriod(company.getId(), project.getId(), "2025-10")).thenReturn(Optional.empty());

        // Mock save để trả về report có ID (cho details)
        when(reportRepository.save(any(EmissionReport.class))).thenAnswer(inv -> {
            EmissionReport r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        String csvData = "period,total_energy,license_plate\n" +
                "2025-10,150.5,51A-12345\n" +
                "2025-10,50.0,51A-67890";
        MockMultipartFile file = new MockMultipartFile("file", "report.csv", "text/csv", csvData.getBytes());

        ArgumentCaptor<EmissionReport> reportCaptor = ArgumentCaptor.forClass(EmissionReport.class);
        ArgumentCaptor<List<EmissionReportDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        EmissionReportResponse response = emissionReportService.uploadCsvAsReport(file, project.getId());

        // Assert
        verify(reportRepository).save(reportCaptor.capture());
        verify(detailRepository).saveAll(detailsCaptor.capture());

        EmissionReport savedReport = reportCaptor.getValue();
        List<EmissionReportDetail> savedDetails = detailsCaptor.getValue();

        assertThat(response).isNotNull();
        assertThat(savedReport.getStatus()).isEqualTo(EmissionStatus.SUBMITTED);
        assertThat(savedReport.getPeriod()).isEqualTo("2025-10");
        assertThat(savedReport.getTotalEnergy()).isEqualByComparingTo(new BigDecimal("200.5"));
        assertThat(savedReport.getVehicleCount()).isEqualTo(2); // 2 biển số khác nhau
        assertThat(savedReport.getUploadStorageUrl()).isEqualTo("url");

        assertThat(savedDetails).hasSize(2);
        assertThat(savedDetails.get(0).getTotalEnergy()).isEqualByComparingTo(new BigDecimal("150.5"));
        // Kiểm tra CO2 được tính theo EF mặc định (0.4)
        assertThat(savedDetails.get(0).getCo2Kg()).isEqualByComparingTo(new BigDecimal("150.5").multiply(new BigDecimal("0.4")));
    }

    @Test
    @DisplayName("[Luồng 2 - Upload] Thất bại khi thiếu cột total_energy")
    void uploadCsvAsReport_Fail_MissingEnergyColumn() {
        // Arrange
        mockSecurityContext(companyUser);
        when(companyRepository.findByUserId(companyUser.getId())).thenReturn(Optional.of(company));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(storage.putObject(anyString(), anyString(), any())).thenReturn(new FileStorageService.PutResult("key", "url"));

        String csvData = "period,license_plate\n2025-10,51A-12345";
        MockMultipartFile file = new MockMultipartFile("file", "report.csv", "text/csv", csvData.getBytes());

        // Act & Assert
        assertThatThrownBy(() -> emissionReportService.uploadCsvAsReport(file, project.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CSV_MISSING_TOTAL_ENERGY_OR_CHARGING);
    }

    @Test
    @DisplayName("[Luồng 2 - Verify] CVA duyệt báo cáo thành công (verifyReport)")
    void verifyReport_Success_Approve() {
        // Arrange
        mockSecurityContext(cvaUser);

        when(cvaRepository.findByEmail(cvaUser.getEmail())).thenReturn(Optional.of(cva));
        when(reportRepository.findById(submittedReport.getId())).thenReturn(Optional.of(submittedReport));
        doNothing().when(notificationService).sendCvaDecision(any(), any(), any(), any(), any(), eq(true), any());

        ArgumentCaptor<EmissionReport> captor = ArgumentCaptor.forClass(EmissionReport.class);

        // Act
        emissionReportService.verifyReport(submittedReport.getId(), true, "CVA OK");

        // Assert
        verify(reportRepository).save(captor.capture());
        EmissionReport savedReport = captor.getValue();

        assertThat(savedReport.getStatus()).isEqualTo(EmissionStatus.CVA_APPROVED);
        assertThat(savedReport.getComment()).isEqualTo("CVA OK");
        assertThat(savedReport.getVerifiedByCva()).isEqualTo(cva);
        verify(notificationService).sendCvaDecision(
                eq(company.getUser().getEmail()),
                eq(company.getCompanyName()),
                eq(submittedReport.getId()),
                eq(project.getTitle()),
                eq(cva.getOrganization()),
                eq(true),
                eq("CVA OK")
        );
    }

    @Test
    @DisplayName("[Luồng 2 - Admin Approve] Admin duyệt báo cáo thành công (adminApproveReport)")
    void adminApproveReport_Success() {
        // Arrange
        submittedReport.setStatus(EmissionStatus.CVA_APPROVED); // Điều kiện: CVA đã duyệt
        when(reportRepository.findById(submittedReport.getId())).thenReturn(Optional.of(submittedReport));
        doNothing().when(notificationService).sendAdminDecision(any(), any(), any(), any(), any(), eq(true), any());
        ArgumentCaptor<EmissionReport> captor = ArgumentCaptor.forClass(EmissionReport.class);

        // Act
        emissionReportService.adminApproveReport(submittedReport.getId(), true, "Admin OK");

        // Assert
        verify(reportRepository).save(captor.capture());
        EmissionReport savedReport = captor.getValue();

        assertThat(savedReport.getStatus()).isEqualTo(EmissionStatus.ADMIN_APPROVED);
        assertThat(savedReport.getComment()).isEqualTo("Admin OK");
        verify(notificationService).sendAdminDecision(any(), any(), any(), any(), any(), eq(true), any());
    }

    @Test
    @DisplayName("[Luồng 2 - Admin Approve] Thất bại khi duyệt báo cáo chưa qua CVA (SUBMITTED)")
    void adminApproveReport_Fail_NotCvaApproved() {
        // Arrange
        submittedReport.setStatus(EmissionStatus.SUBMITTED); // [Test Case] Chưa qua CVA
        when(reportRepository.findById(submittedReport.getId())).thenReturn(Optional.of(submittedReport));

        // Act & Assert
        assertThatThrownBy(() -> emissionReportService.adminApproveReport(submittedReport.getId(), true, "Admin OK"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }
}

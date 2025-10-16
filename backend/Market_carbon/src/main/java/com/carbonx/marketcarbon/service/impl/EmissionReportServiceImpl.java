package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.request.AdminDecisionRequest;
import com.carbonx.marketcarbon.dto.request.CvaReviewRequest;
import com.carbonx.marketcarbon.dto.request.EvidenceCheckRequest;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.dto.response.EvidenceFileDto;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.EmissionReportService;
import com.carbonx.marketcarbon.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmissionReportServiceImpl implements EmissionReportService {

    // Repositories & services cần dùng
    private final EmissionReportRepository reportRepo;
    private final CompanyRepository companyRepo;
    private final ChargingDataRepository chargingRepo;
    private final ProjectRepository projectRepo;

    // Nếu cần ở các chức năng khác thì giữ lại, còn không có thể bỏ bớt
    private final VehicleRepository vehicleRepo;
    private final EvidenceFileRepository evidenceRepo;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final S3StorageServiceImpl s3StorageServiceImpl;

    // ======= Hằng số & parse kỳ =======
    private static final BigDecimal EMISSION_FACTOR_KG_PER_KWH = new BigDecimal("0.5"); // ví dụ

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern QUARTER = Pattern.compile("^(\\d{4})-Q([1-4])$");

    private static final class TimeRange {
        final LocalDateTime from;
        final LocalDateTime to;
        private TimeRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }
    }

    private TimeRange resolvePeriod(String period) {
        // YYYY-MM
        try {
            YearMonth ym = YearMonth.parse(period, YM);
            LocalDateTime from = ym.atDay(1).atStartOfDay();
            LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
            return new TimeRange(from, to);
        } catch (Exception ignore) {}

        // YYYY-Qn
        Matcher m = QUARTER.matcher(period);
        if (m.matches()) {
            int year = Integer.parseInt(m.group(1));
            int q = Integer.parseInt(m.group(2));
            int startMonth = switch (q) { case 1 -> 1; case 2 -> 4; case 3 -> 7; default -> 10; };
            LocalDateTime from = YearMonth.of(year, startMonth).atDay(1).atStartOfDay();
            LocalDateTime to = YearMonth.of(year, startMonth).plusMonths(3).atDay(1).atStartOfDay();
            return new TimeRange(from, to);
        }

        log.error("Invalid period format: {}", period);
        throw new AppException(ErrorCode.CSV_INVALID_FILE_FORMAT); // hoặc error code chuyên biệt cho period
    }

    // ======= Generate report (company aggregate) =======

    @Override
    public EmissionReport generateReport(Long companyId, String period) {
        return generateReport(companyId, period, "VF"); // default project code
    }

    @Override
    public EmissionReport generateReport(Long companyId, String period, String projectTitle) {
//        Company company = companyRepo.findById(companyId)
//                .orElseThrow(() -> new ResourceNotFoundException("Company not found: id = " + companyId));
//
//        TimeRange range = resolvePeriod(period);
//
//        // Tổng hợp năng lượng theo toàn bộ vehicle của company trong khoảng thời gian
//        List<ChargingData> rows = chargingRepo
//                .findByVehicle_Company_IdAndTimestampBetween(companyId, range.from, range.to);
//
//        BigDecimal totalEnergy = rows.stream()
//                .map(ChargingData::getChargingEnergy)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Quy đổi CO₂
//        BigDecimal totalCo2 = totalEnergy.multiply(EMISSION_FACTOR_KG_PER_KWH);
//
//        Project project = projectRepo.findByTitle(projectTitle)
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//
//        // Tìm report kỳ này theo seller(company) + period
//        EmissionReport report = reportRepo.findBySeller_IdAndPeriod(companyId, period);
//        if (report == null) {
//            report = EmissionReport.builder()
//                    .seller(company)
//                    .project(project)
//                    .period(period)
//                    // mapping theo entity hiện tại
//                    .calculatedCo2(totalCo2)
//                    .evCo2(totalCo2)           // nếu cần baseline ICE thì tính thêm, còn không có thể để null
//                    .status(EmissionStatus.SUBMITTED)
//                    .submittedAt(java.time.OffsetDateTime.now())
//                    .createdAt(java.time.OffsetDateTime.now())
//                    .updatedAt(java.time.OffsetDateTime.now())
//                    // CHÚ Ý: nếu EmissionReport.vehicle là bắt buộc thì model cần sửa thành nullable,
//                    // vì đây là report tổng hợp theo công ty.
//                    .build();
//        } else {
//            // chỉ cho phép update khi còn ở trạng thái nháp/đã nộp
//            if (report.getStatus() != EmissionStatus.DRAFT && report.getStatus() != EmissionStatus.SUBMITTED) {
//                throw new AppException(ErrorCode.REPORT_INVALID_STATE);
//            }
//            report.setProject(project);
//            report.setCalculatedCo2(totalCo2);
//            report.setEvCo2(totalCo2);
//            report.setStatus(EmissionStatus.SUBMITTED);
//            report.setSubmittedAt(java.time.OffsetDateTime.now());
//            report.setUpdatedAt(java.time.OffsetDateTime.now());
//        }
//
//        EmissionReport saved = reportRepo.save(report);
//        log.info("Generated report company={}, period={}, energy(kWh)={}, co2(kg)={}",
//                companyId, period, totalEnergy, totalCo2);
          return null;
    }


    // ======= Verify (tối thiểu, không đoán enum lạ để tránh lỗi biên dịch) =======
    @Override
    public EmissionReport verifyReport(Long reportId, Long verifierUserId) {
        EmissionReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != EmissionStatus.SUBMITTED) {
            throw new AppException(ErrorCode.REPORT_INVALID_STATE);
        }

        // Nếu bạn có enum riêng cho trạng thái đã review (VD: CVA_REVIEWED),
        // hãy đổi dòng dưới cho đúng enum của bạn.
        report.setStatus(EmissionStatus.SUBMITTED); // placeholder: không đổi trạng thái để tránh không compile
        report.setUpdatedAt(java.time.OffsetDateTime.now());
        return reportRepo.save(report);
    }

//    // ========= (Giữ nguyên các method interface khác nếu có) =========
//    @Override
//    public EvidenceFile cvaCheckEvidence(Long evidenceId, EvidenceCheckRequest req, Long cvaUserId) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }
//
//    @Override
//    public EmissionReport cvaReview(Long reportId, CvaReviewRequest req, Long cvaUserId) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }
//
//    @Override
//    public EmissionReport adminDecision(Long reportId, AdminDecisionRequest req, Long adminUserId) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }
//
//    @Override
//    public org.springframework.data.domain.Page<EmissionReport> listForCva(com.carbonx.marketcarbon.dto.request.ReportFilter f) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }
//
//    @Override
//    public org.springframework.data.domain.Page<EmissionReport> listForAdmin(com.carbonx.marketcarbon.dto.request.ReportFilter f) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }

    // ======= CSV builder nếu bạn cần cho createAndSubmit (đang không dùng) =======
    private String buildReportCsv(EmissionReport r, Company c, Vehicle v, String sellerEmail) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("ReportID,Period,CalculatedCO2,BaselineICECO2,EVCO2,Status,Company,VehiclePlate,SellerEmail,CreatedAt,SubmittedAt\n");
//        sb.append(csv(r.getId())).append(',')
//                .append(csv(r.getPeriod())).append(',')
//                .append(csv(r.getCalculatedCo2())).append(',')
//                .append(csv(r.getBaselineIceCo2())).append(',')
//                .append(csv(r.getEvCo2())).append(',')
//                .append(csv(r.getStatus())).append(',')
//                .append(csv(c.getCompanyName())).append(',')
//                .append(csv(v != null ? v.getPlateNumber() : null)).append(',')
//                .append(csv(sellerEmail)).append(',')
//                .append(csv(r.getCreatedAt())).append(',')
//                .append(csv(r.getSubmittedAt())).append('\n');
          return null;
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}

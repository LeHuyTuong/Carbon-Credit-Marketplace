package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.response.EmissionReportDetailResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.AiScoringService;
import com.carbonx.marketcarbon.service.EmissionReportService;
import com.carbonx.marketcarbon.service.FileStorageService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmissionReportServiceImpl implements EmissionReportService {

    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final EmissionReportRepository reportRepository;
    private final EmissionReportDetailRepository detailRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final AiScoringService aiScoringService;
    private final CvaRepository cvaRepository;

    // Hệ số phát thải mặc định nếu CSV không có cột CO2
    private static final BigDecimal DEFAULT_EF_KG_PER_KWH = new BigDecimal("0.4");

    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public EmissionReportResponse uploadCsvAsReport(MultipartFile file, Long projectIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User currentUser = Optional.ofNullable(userRepository.findByEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        Company seller = companyRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        log.info("[CSV-UPLOAD] User = {}, Company = {} (ID={})",
                email, seller.getCompanyName(), seller.getId());

        if (projectIdParam == null)
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);

        Project project = projectRepository.findById(projectIdParam)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "emission.csv";
        FileStorageService.PutResult put;
        try {
            put = storage.putObject("emission-reports", filename, file);
        } catch (Exception ex) {
            log.error("[CSV-UPLOAD] Upload storage failed: {}", ex.getMessage(), ex);
            throw new AppException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        String sha256 = computeSha256(file);

        String period = null;
        List<EmissionReportDetail> details = new ArrayList<>();
        int rows = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            Set<String> headers = parser.getHeaderMap().keySet();

            log.info("[CSV-UPLOAD] Headers: {}", headers);

            // Các header cần thiết
            String periodHeader = requireHeader(headers, "period");
            String energyHeader = findHeader(headers, "total_energy", "total_charging_energy",
                    "charging_energy_total", "charging_energy");

            if (energyHeader == null)
                throw new AppException(ErrorCode.CSV_MISSING_TOTAL_ENERGY_OR_CHARGING);

            String co2Header = findHeader(headers, "co2_kg", "co2", "co2e_kg");
            String plateHeader = findHeader(headers, "license_plate", "plate", "plate_number",
                    "vehicle_plate", "bien_so", "bien_so_xe");

            for (CSVRecord r : parser) {
                String per = safeGet(r, periodHeader);
                if (period == null) period = per;
                else if (!period.equals(per))
                    throw new AppException(ErrorCode.CSV_INCONSISTENT_PERIOD);

                String energyStr = safeGet(r, energyHeader);
                if (energyStr.isEmpty())
                    throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);

                BigDecimal energy = new BigDecimal(energyStr);

                BigDecimal co2Kg = null;
                if (co2Header != null) {
                    String c = safeGet(r, co2Header);
                    if (!c.isEmpty()) co2Kg = new BigDecimal(c);
                }

                String licensePlate = null;
                if (plateHeader != null) {
                    String p = safeGet(r, plateHeader);
                    if (!p.isBlank()) licensePlate = p.trim();
                }

                details.add(EmissionReportDetail.builder()
                        .period(per)
                        .companyId(seller.getId())
                        .projectId(projectIdParam)
                        .vehiclePlate(licensePlate)
                        .totalEnergy(energy)
                        .co2Kg(co2Kg)
                        .build());
                rows++;
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CSV-UPLOAD] Parse error: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
        }

        if (details.isEmpty())
            throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);

        if (reportRepository.findBySellerIdAndProjectIdAndPeriod(seller.getId(), project.getId(), period).isPresent()) {
            throw new AppException(ErrorCode.REPORT_DUPLICATE_PERIOD);
        }

        for (EmissionReportDetail d : details) {
            if (d.getCo2Kg() == null)
                d.setCo2Kg(d.getTotalEnergy().multiply(DEFAULT_EF_KG_PER_KWH));
        }

        BigDecimal totalEnergy = details.stream()
                .map(EmissionReportDetail::getTotalEnergy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCo2 = details.stream()
                .map(EmissionReportDetail::getCo2Kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long distinctPlates = details.stream()
                .map(EmissionReportDetail::getVehiclePlate)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet())
                .size();

        int vehicleCount = (distinctPlates > 0) ? (int) distinctPlates : details.size();

        EmissionReport report = EmissionReport.builder()
                .seller(seller)
                .project(project)
                .period(period)
                .totalEnergy(totalEnergy)
                .totalCo2(totalCo2)
                .status(EmissionStatus.SUBMITTED)
                .source("CSV")
                .vehicleCount(vehicleCount)
                .uploadOriginalFilename(filename)
                .uploadMimeType(file.getContentType())
                .uploadSizeBytes(file.getSize())
                .uploadSha256(sha256)
                .uploadStorageKey(put.key())
                .uploadStorageUrl(put.url())
                .uploadRows(rows)
                .createdAt(OffsetDateTime.now())
                .submittedAt(OffsetDateTime.now())
                .build();

        EmissionReport saved = reportRepository.save(report);

        for (EmissionReportDetail d : details) d.setReport(saved);
        detailRepository.saveAll(details);

        log.info("[CSV-UPLOAD] Report {} uploaded successfully for company '{}' ({}) — period={}, rows={}, vehicles={}",
                saved.getId(), seller.getCompanyName(), seller.getId(), period, rows, vehicleCount);

        return EmissionReportResponse.from(saved);
    }
    @Override
    public Page<EmissionReportResponse> listReportsForCva(Pageable pageable) {
        return reportRepository.findBySourceIgnoreCaseAndStatus("CSV", EmissionStatus.SUBMITTED, pageable)
                .map(EmissionReportResponse::from);
    }

    @Override
    public byte[] downloadCsv(Long reportId) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        if (r.getUploadStorageKey() == null) throw new AppException(ErrorCode.STORAGE_READ_FAILED);
        try {
            return storage.getObject(r.getUploadStorageKey());
        } catch (Exception ex) {
            throw new AppException(ErrorCode.STORAGE_READ_FAILED);
        }
    }

    @Override
    public byte[] exportSummaryCsv(Long reportId) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        StringBuilder sb = new StringBuilder();
        sb.append("project_id,period,total_energy,total_vehicles,total_co2,total_ev_owner\n");
        sb.append(r.getProject().getId()).append(',')
                .append(r.getPeriod()).append(',')
                .append(r.getTotalEnergy().toPlainString()).append(',')
                .append(r.getVehicleCount()).append(',')
                .append(r.getTotalCo2().toPlainString()).append(',')
                .append(r.getVehicleCount()).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public EmissionReportResponse verifyReport(Long reportId, boolean approved, String comment) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Cva cva = cvaRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        r.setVerifiedByCva(cva);
        r.setVerifiedAt(OffsetDateTime.now());
        r.setComment(comment);
        r.setStatus(approved ? EmissionStatus.CVA_APPROVED : EmissionStatus.REJECTED);
        r.setUpdatedAt(OffsetDateTime.now());

        reportRepository.save(r);

        return EmissionReportResponse.from(r);
    }


    @Override
    public EmissionReportResponse adminApproveReport(Long reportId, boolean approved, String note) {
        // Tìm báo cáo bằng ID
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        // Kiểm tra trạng thái hiện tại trước khi thay đổi
        EmissionStatus currentStatus = r.getStatus();

        // Chỉ cho phép duyệt khi báo cáo có trạng thái là ADMIN_APPROVED, ADMIN_REJECTED, hoặc CVA_APPROVED
        if (!(currentStatus == EmissionStatus.ADMIN_APPROVED ||
                currentStatus == EmissionStatus.ADMIN_REJECTED ||
                currentStatus == EmissionStatus.CVA_APPROVED)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Cập nhật thông tin báo cáo
        r.setApprovedAt(OffsetDateTime.now());
        r.setComment(note);
        r.setStatus(approved ? EmissionStatus.ADMIN_APPROVED : EmissionStatus.ADMIN_REJECTED);

        // Lưu lại báo cáo
        reportRepository.save(r);

        // Trả về response
        return EmissionReportResponse.from(r);
    }


    @Override
    public Page<EmissionReportResponse> listReportsForAdmin(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return reportRepository.findAll(pageable).map(EmissionReportResponse::from);
        }
        final EmissionStatus parsed;
        try {
            parsed = EmissionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        return reportRepository.findByStatus(parsed, pageable).map(EmissionReportResponse::from);
    }

    @Override
    public EmissionReportResponse getById(Long reportId) {
        EmissionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        return EmissionReportResponse.from(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmissionReportDetailResponse> getReportDetails(Long reportId, String plateContains, Pageable pageable) {
        EmissionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCompany = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"));
        // CVA được phép xem mọi report -> không cần check companyId

        if (isCompany) {
            Long companyId = currentCompanyId();
            if (!report.getSeller().getId().equals(companyId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        Page<EmissionReportDetail> page = (plateContains != null && !plateContains.isBlank())
                ? detailRepository.findByReport_IdAndVehiclePlateContainingIgnoreCase(reportId, plateContains, pageable)
                : detailRepository.findByReport_Id(reportId, pageable);

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        return page.map(EmissionReportDetailResponse::from);
    }



    @Override
    public EmissionReportResponse aiSuggestScore(Long reportId) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        // chỉ CVA/ADMIN được phép gợi ý
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_CVA") || a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        var details = detailRepository.findByReport(r);
        AiScoringService.AiScoreResult rs = aiScoringService.suggestScore(r, details);

        r.setAiPreScore(rs.score());
        r.setAiPreNotes(rs.notes());
        r.setAiVersion(rs.version());
        r.setUpdatedAt(OffsetDateTime.now());
        reportRepository.save(r);

        return EmissionReportResponse.from(r);
    }

    @Override
    public EmissionReportResponse verifyReportWithScore(Long reportId, BigDecimal score, boolean approved, String comment) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Cva cva = cvaRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        r.setVerifiedByCva(cva);
        r.setVerifiedAt(OffsetDateTime.now());
        r.setVerificationScore(score);
        r.setVerificationComment(comment);
        r.setStatus(approved ? EmissionStatus.CVA_APPROVED : EmissionStatus.REJECTED);
        r.setUpdatedAt(OffsetDateTime.now());

        reportRepository.save(r);

        return EmissionReportResponse.from(r);
    }


    @Override
    public List<EmissionReportResponse> listReportsForCompany(String status, Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        List<EmissionReport> reports;

        if (projectId != null && status != null && !status.isBlank()) {
            EmissionStatus st = EmissionStatus.valueOf(status.toUpperCase());
            reports = reportRepository.findBySeller_IdAndProject_IdAndStatus(company.getId(), projectId, st);
        } else if (projectId != null) {
            reports = reportRepository.findBySeller_IdAndProject_Id(company.getId(), projectId);
        } else if (status != null && !status.isBlank()) {
            EmissionStatus st = EmissionStatus.valueOf(status.toUpperCase());
            reports = reportRepository.findBySeller_IdAndStatus(company.getId(), st);
        } else {
            reports = reportRepository.findBySeller_Id(company.getId());
        }

        return reports.stream().map(EmissionReportResponse::from).toList();
    }


    private static String safeGet(CSVRecord r, String header) {
        String v = r.get(header);
        return v == null ? "" : v.trim();
    }


    private static String findHeader(Set<String> headers, String... candidates) {
        for (String c : candidates) {
            for (String h : headers) {
                if (h.equalsIgnoreCase(c)) return h;
            }
        }
        return null;
    }

    private static String requireHeader(Set<String> headers, String... candidates) {
        String h = findHeader(headers, candidates);
        if (h != null) return h;

        // Map ứng với các cột bắt buộc bạn đang dùng
        boolean askProject = Arrays.stream(candidates).anyMatch(c -> c.equalsIgnoreCase("project_id"));
        boolean askPeriod = Arrays.stream(candidates).anyMatch(c -> c.equalsIgnoreCase("period"));

        if (askProject) {
            throw new AppException(ErrorCode.CSV_MISSING_PROJECT_ID);
        }
        if (askPeriod) {
            throw new AppException(ErrorCode.CSV_MISSING_PERIOD);
        }

        // Fallback an toàn nếu có thêm cột bắt buộc khác sau này
        throw new AppException(ErrorCode.CSV_PARSE_ERROR);
    }


    private static Long parseLong(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        return Long.valueOf(s);
    }


    private String computeSha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

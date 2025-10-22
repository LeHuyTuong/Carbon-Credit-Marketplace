package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.response.EmissionReportDetailResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EmissionReportDetail;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EmissionReportDetailRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.EmissionReportService;
import com.carbonx.marketcarbon.service.FileStorageService;
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

    // Hệ số phát thải mặc định nếu CSV không có cột CO2
    private static final BigDecimal DEFAULT_EF_KG_PER_KWH = new BigDecimal("0.4");

    @Override
    public EmissionReportResponse uploadCsvAsReport(MultipartFile file) {
        // 1) Lấy user & company
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User currentUser = Optional.ofNullable(userRepository.findByEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        Company seller = companyRepository.findByUserId(currentUser.getId()).orElse(null);
        if (seller == null) {
            seller = companyRepository.findByUserEmail(email);
        }
        if (seller == null) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }

        // 2) Lưu file vào storage
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "emission.csv";
        FileStorageService.PutResult put;
        try {
            put = storage.putObject("emission-reports", filename, file);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
        String sha256 = computeSha256(file);

        // 3) Parse CSV -> build list detail theo từng xe
        String period = null;
        Long projectId = null;

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

            // Các header cần/tuỳ chọn
            String projectHeader = requireHeader(headers, "project_id");                 // bắt buộc
            String periodHeader = requireHeader(headers, "period");                     // bắt buộc
            String energyHeader = findHeader(headers,
                    "total_energy", "total_charging_energy", "charging_energy_total", "charging_energy");
            if (energyHeader == null) {
                throw new AppException(ErrorCode.CSV_MISSING_TOTAL_ENERGY_OR_CHARGING);
            }
            String co2Header = findHeader(headers, "co2_kg", "co2", "co2e_kg");     // tuỳ chọn
            String vehicleIdHeader = findHeader(headers, "vehicle_id", "vehicle", "ev_id", "vin"); // khuyến nghị

            for (CSVRecord r : parser) {
                // Kiểm tra project & period nhất quán
                Long pid = parseLong(safeGet(r, projectHeader));
                String per = safeGet(r, periodHeader);

                if (projectId == null) projectId = pid;
                else if (!projectId.equals(pid)) throw new AppException(ErrorCode.CSV_INCONSISTENT_PROJECT_ID);

                if (period == null) period = per;
                else if (!period.equals(per)) throw new AppException(ErrorCode.CSV_INCONSISTENT_PERIOD);

                // Lấy năng lượng
                String energyStr = safeGet(r, energyHeader);
                if (energyStr.isEmpty()) {
                    throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
                }
                BigDecimal energy = new BigDecimal(energyStr);

                // Lấy CO2 (nếu có), nếu không thì để null - sẽ tính sau theo EF mặc định
                BigDecimal co2Kg = null;
                if (co2Header != null) {
                    String c = safeGet(r, co2Header);
                    if (!c.isEmpty()) {
                        co2Kg = new BigDecimal(c);
                    }
                }

                // vehicleId (tuỳ chọn)
                Long vehicleId = null;
                if (vehicleIdHeader != null) {
                    String v = safeGet(r, vehicleIdHeader);
                    if (!v.isEmpty()) {
                        try {
                            vehicleId = parseLong(v);
                        } catch (NumberFormatException ignored) {
                            // nếu VIN alphanumeric -> có thể để null hoặc hash riêng, tuỳ business
                        }
                    }
                }

                EmissionReportDetail d = EmissionReportDetail.builder()
                        .period(per)
                        .companyId(seller.getId())
                        .projectId(pid)
                        .vehicleId(vehicleId)
                        .totalEnergy(energy)
                        .co2Kg(co2Kg) // có thể null, sẽ fill sau
                        .build();

                details.add(d);
                rows++;
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV parse error", e);
            throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
        }

        if (details.isEmpty()) {
            throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
        }

        // 4) Lấy project, check trùng kỳ
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (reportRepository.findBySellerIdAndProjectIdAndPeriod(seller.getId(), project.getId(), period).isPresent()) {
            throw new AppException(ErrorCode.REPORT_DUPLICATE_PERIOD);
        }

        // 5) Tính CO2 cho từng detail nếu thiếu, dùng EF mặc định 0.4 kg/kWh
        for (EmissionReportDetail d : details) {
            if (d.getCo2Kg() == null) {
                d.setCo2Kg(d.getTotalEnergy().multiply(DEFAULT_EF_KG_PER_KWH));
            }
        }

        // 6) Tổng hợp
        BigDecimal totalEnergy = details.stream()
                .map(EmissionReportDetail::getTotalEnergy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCo2 = details.stream()
                .map(EmissionReportDetail::getCo2Kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // vehicleCount: nếu có vehicle_id -> đếm distinct; nếu không -> số dòng
        long distinctVehicles = details.stream()
                .map(EmissionReportDetail::getVehicleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
        int vehicleCount = (distinctVehicles > 0) ? (int) distinctVehicles : details.size();

        // 7) Lưu report tổng
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

        // 8) Gắn report_id cho chi tiết và lưu
        for (EmissionReportDetail d : details) {
            d.setReport(saved);
        }
        detailRepository.saveAll(details);

        return EmissionReportResponse.from(saved);
    }

    @Override
    public Page<EmissionReportResponse> listReportsForCva(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return reportRepository.findBySourceIgnoreCase("CSV", pageable).map(EmissionReportResponse::from);
        }
        EmissionStatus st = EmissionStatus.valueOf(status.toUpperCase());
        return reportRepository.findBySourceIgnoreCaseAndStatus("CSV", st, pageable).map(EmissionReportResponse::from);
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
        User verifier = Optional.ofNullable(userRepository.findByEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        r.setVerifiedBy(verifier);
        r.setVerifiedAt(OffsetDateTime.now());
        r.setComment(comment);
        r.setStatus(approved ? EmissionStatus.CVA_APPROVED : EmissionStatus.REJECTED);
        reportRepository.save(r);
        return EmissionReportResponse.from(r);
    }

    @Override
    public EmissionReportResponse adminApproveReport(Long reportId, boolean approved, String note) {
        EmissionReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        r.setApprovedAt(OffsetDateTime.now());
        r.setComment(note);
        r.setStatus(approved ? EmissionStatus.ADMIN_APPROVED : EmissionStatus.REJECTED);
        reportRepository.save(r);
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
    @PreAuthorize("hasRole('COMPANY')")
    public List<EmissionReportResponse> listReportsForCompany(String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        List<EmissionReport> reports;
        if (status == null || status.isBlank()) {
            reports = reportRepository.findBySeller_Id(company.getId());
        } else {
            EmissionStatus st = EmissionStatus.valueOf(status.toUpperCase());
            reports = reportRepository.findBySeller_IdAndStatus(company.getId(), st);
        }

        return reports.stream().map(EmissionReportResponse::from).toList();
    }

    @Override
    public EmissionReportResponse getById(Long reportId) {
        EmissionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        return EmissionReportResponse.from(report);
    }

    @Override
    public List<EmissionReportDetailResponse> getReportDetails(Long reportId) {
        EmissionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        List<EmissionReportDetail> details = detailRepository.findByReport(report);
        if (details.isEmpty()) {
            throw new AppException(ErrorCode.REPORT_DETAILS_NOT_FOUND);
        }

        return details.stream()
                .map(EmissionReportDetailResponse::from)
                .toList();
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

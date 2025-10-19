package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmissionReportServiceImpl implements EmissionReportService {

    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final EmissionReportRepository reportRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    @Override
    public EmissionReportResponse uploadCsvAsReport(MultipartFile file) {
        // 1) Lấy user & company
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User currentUser = Optional.ofNullable(userRepository.findByEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        Company seller = companyRepository.findByUserId(currentUser.getId()).orElse(null);
        if (seller == null) {
            seller = companyRepository.findByUserEmail(email); // repo của bạn trả về Company (có thể null)
        }
        if (seller == null) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }

        // 2) Lưu file
        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "emission.csv";
        FileStorageService.PutResult put;
        try {
            put = storage.putObject("emission-reports", filename, file);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
        String sha256 = computeSha256(file);

        // 3) Parse CSV
        String period = null;
        Long projectId = null;
        int rows = 0;
        BigDecimal totalEnergy = null; // ưu tiên tổng đã nhập sẵn
        Integer vehicleCount = null;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()               // dùng dòng đầu làm header
                    .setSkipHeaderRecord(true) // bỏ qua dòng header ở dữ liệu
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            Set<String> headers = parser.getHeaderMap().keySet();

            String projectHeader = headers.stream().filter(h -> h.equalsIgnoreCase("project_id")).findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.CSV_MISSING_PROJECT_ID));
            String periodHeader = headers.stream().filter(h -> h.equalsIgnoreCase("period")).findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.CSV_MISSING_PERIOD));

            String totalEnergyHeader = headers.stream().filter(h ->
                    h.equalsIgnoreCase("total_energy")
                            || h.equalsIgnoreCase("total_charging_energy")
                            || h.equalsIgnoreCase("charging_energy_total")).findFirst().orElse(null);

            String chargingEnergyHeader = headers.stream().filter(h -> h.equalsIgnoreCase("charging_energy"))
                    .findFirst().orElse(null); // fallback nếu thiếu total_energy

            String vehicleCountHeader = headers.stream().filter(h ->
                            h.equalsIgnoreCase("total_ev_owner")
                                    || h.equalsIgnoreCase("total_vehicles")
                                    || h.equalsIgnoreCase("total_vehicle")
                                    || h.equalsIgnoreCase("tong_xe")).findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.CSV_MISSING_VEHICLE_COUNT_COLUMN));

            if (totalEnergyHeader == null && chargingEnergyHeader == null) {
                throw new AppException(ErrorCode.CSV_MISSING_TOTAL_ENERGY_OR_CHARGING);
            }

            for (CSVRecord r : parser) {
                Long pid = Long.valueOf(r.get(projectHeader).trim());
                String per = r.get(periodHeader).trim();

                if (projectId == null) projectId = pid;
                else if (!projectId.equals(pid)) throw new AppException(ErrorCode.CSV_INCONSISTENT_PROJECT_ID);

                if (period == null) period = per;
                else if (!period.equals(per)) throw new AppException(ErrorCode.CSV_INCONSISTENT_PERIOD);

                if (totalEnergyHeader != null) {
                    String te = safeGet(r, totalEnergyHeader);
                    if (!te.isEmpty()) {
                        BigDecimal val = new BigDecimal(te);
                        if (totalEnergy == null) totalEnergy = val;
                        else if (totalEnergy.compareTo(val) != 0)
                            throw new AppException(ErrorCode.CSV_INCONSISTENT_TOTAL_ENERGY);
                    }
                }

                String v = safeGet(r, vehicleCountHeader);
                if (!v.isEmpty()) {
                    Integer val;
                    try { val = Integer.valueOf(v); }
                    catch (NumberFormatException ex) { throw new AppException(ErrorCode.CSV_VEHICLE_COUNT_INVALID); }
                    if (vehicleCount == null) vehicleCount = val;
                    else if (!vehicleCount.equals(val)) throw new AppException(ErrorCode.CSV_VEHICLE_COUNT_INVALID);
                }

                rows++;
            }

            // Fallback: cộng dồn charging_energy nếu thiếu total_energy
            if (totalEnergy == null && chargingEnergyHeader != null) {
                BigDecimal sum = BigDecimal.ZERO;
                try (Reader r2 = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
                    CSVParser p2 = format.parse(r2);
                    for (CSVRecord rec : p2) {
                        String ce = safeGet(rec, chargingEnergyHeader);
                        if (!ce.isEmpty()) sum = sum.add(new BigDecimal(ce));
                    }
                }
                totalEnergy = sum;
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            // lỗi parse/IO -> chuẩn hoá về mã tổng năng lượng không tìm thấy
            throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
        }

        if (totalEnergy == null) throw new AppException(ErrorCode.CSV_TOTAL_ENERGY_NOT_FOUND);
        if (vehicleCount == null || vehicleCount < 0) throw new AppException(ErrorCode.CSV_VEHICLE_COUNT_MISSING);

        // 4) Lấy project, check trùng kỳ (dùng isPresent để khỏi cần biến final trong lambda)
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (reportRepository.findBySellerIdAndProjectIdAndPeriod(seller.getId(), project.getId(), period).isPresent()) {
            throw new AppException(ErrorCode.REPORT_DUPLICATE_PERIOD);
        }

        // 5) Tính CO2 (tạm 0.4 kg/kWh — sau này lấy theo cấu hình Project)
        BigDecimal totalCo2 = totalEnergy.multiply(BigDecimal.valueOf(0.4));

        // 6) Lưu report
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

        reportRepository.save(report);
        return EmissionReportResponse.from(report);
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
        // Nếu không truyền status -> lấy toàn bộ
        if (status == null || status.isBlank()) {
            return reportRepository.findAll(pageable)
                    .map(EmissionReportResponse::from);
        }

        // Parse status từ Enum EmissionStatus
        final EmissionStatus parsed;
        try {
            parsed = EmissionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        return reportRepository.findByStatus(parsed, pageable)
                .map(EmissionReportResponse::from);
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public List<EmissionReportResponse> listReportsForCompany(String status) {
        // Lấy thông tin user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        Company company = companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        List<EmissionReport> reports;
        if (status == null || status.isBlank()) {
            // Lấy toàn bộ report của công ty
            reports = reportRepository.findBySeller_Id(company.getId());
        } else {
            // Lọc theo trạng thái nếu có
            EmissionStatus st = EmissionStatus.valueOf(status.toUpperCase());
            reports = reportRepository.findBySeller_IdAndStatus(company.getId(), st);
        }

        // Chuyển sang DTO
        return reports.stream()
                .map(EmissionReportResponse::from)
                .toList();
    }


    private static String safeGet(CSVRecord r, String header) {
        String v = r.get(header);
        return v == null ? "" : v.trim();
    }

    private String computeSha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(file.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }
}

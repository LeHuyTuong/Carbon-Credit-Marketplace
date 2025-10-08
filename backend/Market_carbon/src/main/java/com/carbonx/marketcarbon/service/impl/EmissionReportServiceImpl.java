package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.request.*;
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
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // dùng Spring @Transactional
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmissionReportServiceImpl implements EmissionReportService {

    private final EmissionReportRepository reportRepo;
    private final CompanyRepository companyRepository;
    private final VehicleRepository vehicleRepo;
    private final EvidenceFileRepository evidenceRepo;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final S3StorageServiceImpl s3StorageServiceImpl;

    // ===================== create & submit, rồi sinh CSV cho CVA =====================
    @Override
    @Transactional
    public EmissionReportResponse createAndSubmit(EmissionReportCreateRequest req) {

        // 1) lấy current user từ token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        // 2) lấy company theo user và vehicle theo req
        Company sellerCompany = companyRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SELLER_NOT_FOUND));

        Vehicle vehicle = vehicleRepo.findById(req.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // 3) tạo report trạng thái SUBMITTED
        EmissionReport report = EmissionReport.builder()
                .seller(sellerCompany)
                .vehicle(vehicle)
                .period(req.getPeriod())
                .calculatedCo2(req.getCalculatedCo2())
                .baselineIceCo2(req.getBaselineIceCo2())
                .evCo2(req.getEvCo2())
                .status(EmissionStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .submittedAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        EmissionReport saved = reportRepo.save(report);

        // 4) build CSV
        String csv = buildReportCsv(saved, sellerCompany, vehicle, currentUser.getEmail());
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 5) upload CSV lên S3 (nếu fail -> throw -> rollback DB)
        String key = String.format("emission/%d/%s/report-%d.csv",
                saved.getId(), OffsetDateTime.now().toLocalDate(), saved.getId());

        StorageService.StoredObject obj = storageService.upload(
                key, "text/csv", bytes.length, new ByteArrayInputStream(bytes)
        );

        // 6) lưu EvidenceFile đại diện CSV để CVA duyệt
        EvidenceFile csvEvidence = EvidenceFile.builder()
                .report(saved)
                .fileName("report-" + saved.getId() + ".csv")
                .contentType("text/csv")
                .fileSizeBytes((long) bytes.length)
                .storageUrl(obj.url())
                .storageKey(obj.key())
                .uploadedAt(OffsetDateTime.now())
                .checkedByCva(false)
                .build();
        evidenceRepo.save(csvEvidence);

        // 7) trả response kèm csvUrl
        return EmissionReportResponse.builder()
                .id(saved.getId())
                .period(saved.getPeriod())
                .calculatedCo2(saved.getCalculatedCo2())
                .baselineIceCo2(saved.getBaselineIceCo2())
                .evCo2(saved.getEvCo2())
                .status(saved.getStatus().name())
                .sellerEmail(currentUser.getEmail())
                .sellerCompanyName(sellerCompany.getCompanyName())
                .vehiclePlate(vehicle.getPlateNumber())
                .createdAt(saved.getCreatedAt())
                .submittedAt(saved.getSubmittedAt())
                .csvUrl(obj.url())
                .build();
    }

    // ===================== upload multiple user evidences (đã nâng cấp kèm cleanup) =====================
    @Transactional
    public List<EvidenceFileDto> uploadMultiple(Long reportId, List<MultipartFile> files) {
        EmissionReport r = mustReport(reportId);
        if (r.getStatus() == EmissionStatus.ADMIN_APPROVED || r.getStatus() == EmissionStatus.REJECTED) {
            throw new AppException(ErrorCode.REPORT_INVALID_STATE);
        }
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> uploadedKeys = new ArrayList<>();
        List<EvidenceFile> toPersist = new ArrayList<>();

        try {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;

                String safeName = sanitizeFileName(
                        Objects.requireNonNullElse(f.getOriginalFilename(), "unknown"));
                String contentType = Optional.ofNullable(f.getContentType())
                        .orElse("application/octet-stream");
                long size = f.getSize();

                String key = String.format("emission/%d/%s/%s-%s",
                        r.getId(), LocalDate.now(), UUID.randomUUID(), safeName);

                try (var in = f.getInputStream()) {
                    var obj = s3StorageServiceImpl.upload(key, contentType, size, in);
                    uploadedKeys.add(obj.key());

                    EvidenceFile ev = EvidenceFile.builder()
                            .report(r)
                            .fileName(safeName)
                            .contentType(contentType)
                            .fileSizeBytes(size)
                            .storageUrl(obj.url())
                            .storageKey(obj.key())
                            .uploadedAt(OffsetDateTime.now())
                            .checkedByCva(false)
                            .build();

                    toPersist.add(ev);
                }
            }

            if (toPersist.isEmpty()) return List.of();

            List<EvidenceFile> saved = evidenceRepo.saveAll(toPersist);
            r.setUpdatedAt(OffsetDateTime.now());
            reportRepo.save(r);

            // ➜ Map Entity -> DTO để tránh lazy proxy
            return saved.stream().map(ev -> EvidenceFileDto.builder()
                    .id(ev.getId())
                    .fileName(ev.getFileName())
                    .contentType(ev.getContentType())
                    .fileSizeBytes(ev.getFileSizeBytes())
                    .storageUrl(ev.getStorageUrl())
                    .uploadedAt(ev.getUploadedAt())
                    .checkedByCva(ev.getCheckedByCva())
                    .build()
            ).toList();

        } catch (IOException e) {
            cleanupUploadedS3Objects(uploadedKeys);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (RuntimeException re) {
            cleanupUploadedS3Objects(uploadedKeys);
            throw re;
        }
    }


    private void cleanupUploadedS3Objects(List<String> uploadedKeys) {
        if (uploadedKeys == null || uploadedKeys.isEmpty()) return;
        for (String k : uploadedKeys) {
            try { storageService.delete(k); } catch (Exception ignore) {}
        }
    }

    private static final Pattern SAFE_FILE_CHARS = Pattern.compile("[^A-Za-z0-9._-]+");
    private static String sanitizeFileName(String name) {
        String trimmed = name.length() > 200 ? name.substring(0, 200) : name;
        String sanitized = SAFE_FILE_CHARS.matcher(trimmed).replaceAll("_");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    // ===================== CVA/Admin placeholders (chưa triển khai) =====================
    @Override public EvidenceFile cvaCheckEvidence(Long evidenceId, EvidenceCheckRequest req, Long cvaUserId) { return null; }
    @Override public EmissionReport cvaReview(Long reportId, CvaReviewRequest req, Long cvaUserId) { return null; }
    @Override public EmissionReport adminDecision(Long reportId, AdminDecisionRequest req, Long adminUserId) { return null; }
    @Override public Page<EmissionReport> listForCva(ReportFilter f) { return null; }
    @Override public Page<EmissionReport> listForAdmin(ReportFilter f) { return null; }

    // ===================== helpers =====================
    private EmissionReport mustReport(Long id) {
        return reportRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    /** CSV: 1 header + 1 row, escape an toàn tối thiểu */
    private String buildReportCsv(EmissionReport r, Company c, Vehicle v, String sellerEmail) {
        StringBuilder sb = new StringBuilder();
        sb.append("ReportID,Period,CalculatedCO2,BaselineICECO2,EVCO2,Status,Company,VehiclePlate,SellerEmail,CreatedAt,SubmittedAt\n");
        sb.append(csv(r.getId())).append(',')
                .append(csv(r.getPeriod())).append(',')
                .append(csv(r.getCalculatedCo2())).append(',')
                .append(csv(r.getBaselineIceCo2())).append(',')
                .append(csv(r.getEvCo2())).append(',')
                .append(csv(r.getStatus())).append(',')
                .append(csv(c.getCompanyName())).append(',')
                .append(csv(v.getPlateNumber())).append(',')
                .append(csv(sellerEmail)).append(',')
                .append(csv(r.getCreatedAt())).append(',')
                .append(csv(r.getSubmittedAt())).append('\n');
        return sb.toString();
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}

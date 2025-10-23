package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.EmissionReport;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmissionReportResponse {
    // Core
    Long id;
    Long sellerId;
    String sellerName;
    Long projectId;
    String projectName;
    String period;
    BigDecimal totalEnergy;
    BigDecimal totalCo2;
    Integer vehicleCount;
    String status;
    String source;
    OffsetDateTime submittedAt;

    // File metadata
    String uploadOriginalFilename;
    String uploadMimeType;
    Long uploadSizeBytes;
    String uploadSha256;
    String uploadStorageUrl;
    Integer uploadRows;

    // AI suggestion
    BigDecimal aiPreScore;     // 0..10 (1 chữ số thập phân)
    String aiVersion;          // ví dụ "v1.0"
    String aiPreNotes;         // diễn giải điểm gợi ý

    // CVA verification
    BigDecimal verificationScore;    // 0..10 do CVA nhập
    String verificationComment;
    String verifiedBy;               // email/display name
    OffsetDateTime verifiedAt;

    // Admin approval
    OffsetDateTime approvedAt;
    String adminComment;             // nếu bạn lưu ghi chú admin vào report.comment

    // (Tùy chọn) quick metrics để FE hiển thị chất lượng dữ liệu – không bắt buộc
    Integer zeroEnergyRows;
    Double  co2Coverage;             // 0..1
    BigDecimal avgEf;                // kg/kWh
    BigDecimal avgCo2PerVehicle;

    public static EmissionReportResponse from(EmissionReport r) {
        return EmissionReportResponse.builder()
                .id(r.getId())
                .sellerId(r.getSeller().getId())
                .sellerName(r.getSeller().getCompanyName())
                .projectId(r.getProject().getId())
                .projectName(r.getProject().getTitle())
                .period(r.getPeriod())
                .totalEnergy(r.getTotalEnergy())
                .totalCo2(r.getTotalCo2())
                .vehicleCount(r.getVehicleCount())
                .status(r.getStatus().name())
                .source(r.getSource())
                .submittedAt(r.getSubmittedAt())

                .uploadOriginalFilename(r.getUploadOriginalFilename())
                .uploadMimeType(r.getUploadMimeType())
                .uploadSizeBytes(r.getUploadSizeBytes())
                .uploadSha256(r.getUploadSha256())
                .uploadStorageUrl(r.getUploadStorageUrl())
                .uploadRows(r.getUploadRows())

                .aiPreScore(r.getAiPreScore())
                .aiVersion(r.getAiVersion())
                .aiPreNotes(r.getAiPreNotes())

                .verificationScore(r.getVerificationScore())
                .verificationComment(r.getVerificationComment())
                .verifiedBy(r.getVerifiedBy() != null ? r.getVerifiedBy().getEmail() : null)
                .verifiedAt(r.getVerifiedAt())

                .approvedAt(r.getApprovedAt())
                .adminComment(r.getComment()) // nếu dùng r.getComment() làm ghi chú Admin
                .build();
    }

    /** Optional: dùng khi bạn có sẵn metrics (tính ở service), tránh đụng DB lần 2 */
    public static EmissionReportResponse fromWithMetrics(
            EmissionReport r,
            Integer zeroEnergyRows,
            Double co2Coverage,
            BigDecimal avgEf,
            BigDecimal avgCo2PerVehicle
    ) {
        EmissionReportResponse resp = from(r);
        resp.setZeroEnergyRows(zeroEnergyRows);
        resp.setCo2Coverage(co2Coverage);
        resp.setAvgEf(avgEf);
        resp.setAvgCo2PerVehicle(avgCo2PerVehicle);
        return resp;
    }
}
package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.model.EmissionReport;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class EmissionReportResponse {
    Long id;
    Long sellerId;
    String sellerName;
    Long projectId;
    String projectName;        // Project Title
    String period;
    BigDecimal totalEnergy;    // Charging Energy tổng (kWh)
    BigDecimal totalCo2;
    Integer vehicleCount;      // Tổng xe (Total EV_Owner / total_vehicles)
    String status;
    String source;
    OffsetDateTime submittedAt;

    // Metadata (nếu FE muốn xem)
    String uploadOriginalFilename;
    String uploadMimeType;
    Long uploadSizeBytes;
    String uploadSha256;
    String uploadStorageUrl;
    Integer uploadRows;

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
                .build();
    }
}
package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    LocalDateTime submittedAt;

    // File metadata
    String uploadOriginalFilename;
    String uploadMimeType;
    Long uploadSizeBytes;
    String uploadSha256;
    String uploadStorageUrl;
    Integer uploadRows;

    // AI suggestion
    BigDecimal aiPreScore;
    String aiVersion;
    String aiPreNotes;

    // CVA verification
    BigDecimal verificationScore;
    String verificationComment;
    String verifiedBy;
    LocalDateTime verifiedAt;
    String verifiedByCvaName;

    // Admin approval
    LocalDateTime approvedAt;
    String adminComment;
    String adminApprovedByName;

    // Optional metrics
    Integer zeroEnergyRows;
    Double co2Coverage;
    BigDecimal avgEf;
    BigDecimal avgCo2PerVehicle;


    String waitingFor;

    public static EmissionReportResponse from(EmissionReport r) {
        String waitingFor;

        switch (r.getStatus()) {
            case SUBMITTED ->
                    waitingFor = "Waiting for CVA review — please wait until the CVA evaluates your emission report.";
            case CVA_APPROVED ->
                    waitingFor = "Waiting for Admin approval — your report has been approved by the CVA and is now pending Admin confirmation.";
            case CVA_REJECTED ->
                    waitingFor = "Rejected by CVA — please review the CVA’s feedback, correct your data, and re-upload your report.";
            case ADMIN_APPROVED ->
                    waitingFor = "Approved by Admin — your emission report is fully approved. You can now proceed to credit issuance.";
            case ADMIN_REJECTED ->
                    waitingFor = "Rejected by Admin — please review the Admin’s feedback, update your report, and resubmit.";
            default ->
                    waitingFor = "Unknown status — please contact support or the CVA team for clarification.";
        }

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
                .verifiedBy(r.getVerifiedByCva() != null ? r.getVerifiedByCva().getEmail() : null)
                .verifiedAt(r.getVerifiedAt())
                .verifiedByCvaName(r.getVerifiedByCvaName())

                .approvedAt(r.getApprovedAt())
                .adminComment(r.getComment())
                .adminApprovedByName(r.getAdminApprovedByName())

                .waitingFor(waitingFor)
                .build();
    }

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

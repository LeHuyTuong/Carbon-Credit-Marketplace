package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.dto.response.EvidenceFileDto;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EvidenceFile;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmissionReportService {
//    EmissionReportResponse createAndSubmit(EmissionReportCreateRequest req);
//    List<EvidenceFileDto> uploadMultiple(Long reportId, List<MultipartFile> files);
//    EvidenceFile cvaCheckEvidence(Long evidenceId, EvidenceCheckRequest req, Long cvaUserId);
//    EmissionReport cvaReview(Long reportId, CvaReviewRequest req, Long cvaUserId);
//    EmissionReport adminDecision(Long reportId, AdminDecisionRequest req, Long adminUserId);
//    Page<EmissionReport> listForCva(ReportFilter f);
//    Page<EmissionReport> listForAdmin(ReportFilter f);

    /** Tạo/cập nhật báo cáo cho kỳ (mặc định projectCode = "VF"). */
    EmissionReport generateReport(Long companyId, String period);

    /** Tạo/cập nhật báo cáo cho kỳ với projectCode tuỳ chọn. */
    EmissionReport generateReport(Long companyId, String period, String projectTitle);

    /** Xác minh (verify) báo cáo. */
    EmissionReport verifyReport(Long reportId, Long verifierUserId);
}

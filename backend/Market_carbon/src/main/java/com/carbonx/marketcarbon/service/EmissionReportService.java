package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.EmissionReportDetailResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface EmissionReportService {

    EmissionReportResponse uploadCsvAsReport(MultipartFile file, Long projectId);                    // SELLER upload CSV -> SUBMITTED
    Page<EmissionReportResponse> listReportsForCva(Pageable pageable);                               // CVA chỉ thấy SUBMITTED
    byte[] downloadCsv(Long reportId);                                                               // tải file gốc
    byte[] exportSummaryCsv(Long reportId);                                                          // export CSV tóm tắt 1 dòng
    EmissionReportResponse verifyReport(Long reportId, boolean approved, String comment);            // CVA xác minh
    EmissionReportResponse adminApproveReport(Long reportId, boolean approved, String note);         // ADMIN duyệt
    Page<EmissionReportResponse> listReportsForAdmin(String status, Pageable pageable);              // ADMIN có thể lọc theo status
    EmissionReportResponse getById(Long reportId);
    Page<EmissionReportDetailResponse> getReportDetails(Long reportId, String plateContains, Pageable pageable);
    EmissionReportResponse aiSuggestScore(Long reportId);
    EmissionReportResponse verifyReportWithScore(Long reportId, BigDecimal score, boolean approved, String comment);
    List<EmissionReportResponse> listReportsForCompany(String status, Long projectId);               // COMPANY xem report theo dự án
}


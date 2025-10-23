package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.EmissionReportDetailResponse;
import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
import com.carbonx.marketcarbon.dto.response.EvidenceFileDto;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.EvidenceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface EmissionReportService {


    EmissionReportResponse uploadCsvAsReport(MultipartFile file, Long projectId);                    // SELLER upload CSV -> SUBMITTED
    Page<EmissionReportResponse> listReportsForCva(String status, Pageable pageable); // CVA list source=CSV
    byte[] downloadCsv(Long reportId);                                               // tải file gốc
    byte[] exportSummaryCsv(Long reportId);                                          // export CSV tóm tắt 1 dòng
    EmissionReportResponse verifyReport(Long reportId, boolean approved, String comment);  // CVA
    EmissionReportResponse adminApproveReport(Long reportId, boolean approved, String note);
    Page<EmissionReportResponse> listReportsForAdmin(String status, Pageable pageable);
    List<EmissionReportResponse> listReportsForCompany(String status);
    EmissionReportResponse getById(Long reportId);
    List<EmissionReportDetailResponse> getReportDetails(Long reportId);
    EmissionReportResponse aiSuggestScore(Long reportId);
    public EmissionReportResponse verifyReportWithScore(Long reportId, BigDecimal score, boolean approved, String comment);
    List<EmissionReportResponse> listReportsForCompany(String status, Long projectId);

}

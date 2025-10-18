//package com.carbonx.marketcarbon.controller;
//
//import com.carbonx.marketcarbon.dto.request.EmissionReportCreateRequest;
//import com.carbonx.marketcarbon.dto.response.EmissionReportResponse;
//import com.carbonx.marketcarbon.model.EmissionReport;
//import com.carbonx.marketcarbon.model.EvidenceFile;
//import com.carbonx.marketcarbon.service.EmissionReportService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//// === controller: Seller (Company) tạo & upload; CVA list/check/approve; Admin list/decide
//@RestController
//@RequestMapping("/api/emission-reports")
//@RequiredArgsConstructor
//public class EmissionReportController {
//    private final EmissionReportService report;
//
//    @PostMapping
//    @PreAuthorize("hasRole('COMPANY')")
//    public EmissionReportResponse createAndSubmit(@RequestBody EmissionReportCreateRequest req) {
//        return report.createAndSubmit(req);
//    }
//
//    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('COMPANY')")
//    public List<EvidenceFile> uploadMulti(@PathVariable("id") Long id, @RequestParam("files") List<MultipartFile> files) {
//        System.out.println("=== [UPLOAD] Bắt đầu upload cho reportId=" + id);
//        if (files == null || files.isEmpty()) {
//            System.out.println(" Không có file nào được gửi lên!");
//        } else {
//            System.out.println(" Tổng số file nhận được: " + files.size());
//            files.forEach(f -> System.out.println(" - " + f.getOriginalFilename() + " (" + f.getSize() + " bytes, type=" + f.getContentType() + ")"));
//        }
//
//        List<EvidenceFile> result = report.uploadMultiple(id, files);
//        System.out.println(" Upload thành công " + result.size() + " file cho reportId=" + id);
//        return result;
//    }
//
//}
//

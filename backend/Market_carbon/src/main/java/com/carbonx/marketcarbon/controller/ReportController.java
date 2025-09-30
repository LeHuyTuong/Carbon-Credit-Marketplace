package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.config.Translator;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.ReportService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @Autowired
    private  ReportService reportService;
    @Autowired
    private UserRepository userRepository;


    //Client chỉ click link là tải; nếu gọi bằng code, dùng fetch/
    //Mục đích: DTO “gói” thông tin file để trả về trong JSON.
    //fileName: tên gợi ý khi lưu.
    //contentType: loại MIME (ở đây "application/pdf").
    //base64: nội dung file (bytes) đã mã hoá base64 để đi trong JSON.
    record FilePayload(String fileName, String contentType, String base64) {}

    @GetMapping(
            value="/download-json/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<FilePayload>> getDowloadReport(
            @PathVariable("id") Long userId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace) {
        String trace =   requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        User user = userRepository.findById(userId).
                orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("user.not.found")));

        //Gọi service tạo PDF bytes
        byte[] pdf = reportService.generatePdf(user);
        //Vì trả JSON, không thể đẩy bytes thô → mã hoá base64.
       String b64 = java.util.Base64.getEncoder().encodeToString(pdf);
       //Chuẩn bị DTO để client biết: tên file, content-type, dữ liệu.
       FilePayload payload = new FilePayload("certificate-" + user.getId() + ".pdf",
               "application/pdf",
               b64);
        return ResponseEntity.ok(ResponseUtil.success(trace,payload));
    }

    // -- TEST POSTMAN WITHOUT FRONTEND
    @GetMapping("/download/{id}")
    public void downloadRaw(
            @PathVariable("id") Long userId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            jakarta.servlet.http.HttpServletResponse resp) throws IOException {

        String trace = (requestTrace != null) ? requestTrace : java.util.UUID.randomUUID().toString();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("user.not.found") ));

        byte[] pdf = reportService.generatePdf(user);
        String fileName = "certificate-" + user.getId() + ".pdf";

        resp.setHeader("X-Request-Trace", trace);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setContentType("application/pdf");
        resp.setContentLength(pdf.length);
        resp.getOutputStream().write(pdf);
        resp.flushBuffer();
    }










}

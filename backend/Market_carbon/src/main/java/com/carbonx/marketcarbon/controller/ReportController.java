package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.config.Translator;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.StorageException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.FileService;
import com.carbonx.marketcarbon.service.ReportService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final FileService fileService;

    record FilePayload(String fileName, String contentType, String base64) {
    }

    @Operation(summary = "Download file JSON for Company", description = "API help CVA download file JSON for Company")
    @GetMapping(
            value = "/download-json/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<FilePayload>> getDowloadReport(
            @PathVariable("id") Long userId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
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
        return ResponseEntity.ok(ResponseUtil.success(trace, payload));
    }

    @Operation(summary = "Download file PDF for Company ", description = "API help CVA download file PDF for Company ")
    @GetMapping("/download/{id}")
    public void downloadRaw(
            @PathVariable("id") Long userId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            jakarta.servlet.http.HttpServletResponse resp) throws IOException {

        String trace = (requestTrace != null) ? requestTrace : java.util.UUID.randomUUID().toString();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("user.not.found")));

        byte[] pdf = reportService.generatePdf(user);
        String fileName = "certificate-" + user.getId() + ".pdf";

        resp.setHeader("X-Request-Trace", trace);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setContentType("application/pdf");
        resp.setContentLength(pdf.length);
        resp.getOutputStream().write(pdf);
        resp.flushBuffer();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<String>> uploadFile(
             @RequestParam("uploaderType") String uploaderType,
            @RequestParam("uploaderId") Long uploaderId,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Trace", required = false) String trace
    ) {
        String requestTrace = trace != null ? trace : UUID.randomUUID().toString();
        String path;
        //nếu là công ty thì sẽ upfile  có tên công ty
        if ("COMPANY".equalsIgnoreCase(uploaderType)) {
            Company company = companyRepository.findById(uploaderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            path = "upload/company/" + company.getId() + company.getCreateAt();
        }
        // nếu là CVA thì upload file có tên
        else if ("CVA".equalsIgnoreCase(uploaderType)) {
            User cva = userRepository.findById(uploaderId)
                    .orElseThrow(() -> new ResourceNotFoundException("CVA not found"));
            if (companyId == null)
                throw new StorageException("CVA must specify companyId to upload report");
            path = "upload/cva/" + cva.getId() + "/company/" + companyId;
        } else {
            throw new StorageException("Unknown uploader type");
        }

        fileService.storeTo(file, path); // dùng method mở rộng cho phép truyền path
        return ResponseEntity.ok(ResponseUtil.success(requestTrace,
                "File uploaded successfully to " + path));

    }

    @GetMapping("/files/download")
    public ResponseEntity<Resource> download(@RequestParam("key") String fileKey) throws IOException {
        // 1) Kiểm tra phân quyền ở đây (Admin/CVA/Company có quyền?)
        // 2) Tải resource
        Resource resource = fileService.loadAsResource(fileKey);

        Path p = Paths.get(resource.getURI());
        String contentType = Files.probeContentType(p);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // Tên đẹp khi save
        String downloadName = p.getFileName().toString().replaceFirst("^[0-9a-f\\-]{36}_", ""); // bỏ UUID_

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "attachment; filename=\"" + downloadName + "\"")
                .contentLength(Files.size(p))
                .body(resource);
    }



}

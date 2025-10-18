package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class FileUploadTestController {

    private final S3Service s3Service;

    @PostMapping(value = "/upload1e", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<String> uploadTest(@RequestParam("files") List<MultipartFile> files) {
        System.out.println("=== [TEST-UPLOAD] Nhận được " + files.size() + " file ===");
        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            System.out.println("Uploading: " + file.getOriginalFilename());
            String url = s3Service.uploadFile(file);
            urls.add(url);
        }

        System.out.println(" Hoàn tất upload thử, trả về URL list.");
        return urls;
    }
}

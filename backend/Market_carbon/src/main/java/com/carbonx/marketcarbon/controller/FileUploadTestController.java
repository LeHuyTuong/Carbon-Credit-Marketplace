package com.carbonx.marketcarbon.controller;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class FileUploadTestController {

    private final S3Service s3Service;

    @PostMapping(value = "/upload1e", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<String> uploadTest(@RequestParam("files") List<MultipartFile> files) {
        log.debug("Test upload received {} file(s)", files.size());
        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            log.debug("Uploading: {}", file.getOriginalFilename());
            String url = s3Service.uploadFile(file);
            urls.add(url);
        }

        log.debug("Test upload finished, returning URL list.");
        return urls;
    }
}

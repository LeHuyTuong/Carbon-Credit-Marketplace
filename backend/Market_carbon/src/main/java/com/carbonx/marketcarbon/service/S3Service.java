package com.carbonx.marketcarbon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.public-base-url}")
    private String publicBaseUrl;

    public String uploadFile(MultipartFile file) {
        String key = "test/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        try {
            Path temp = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(temp);

            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putReq, temp);
            Files.deleteIfExists(temp);

            return publicBaseUrl + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
}
package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.FileStorageService;
import com.carbonx.marketcarbon.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final StorageService storageService; // S3StorageServiceImpl đã có
    private final S3Client s3;                   // dùng cho getObject

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public PutResult putObject(String key, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            var stored = storageService.upload(
                    key,
                    file.getContentType(),
                    file.getSize(),
                    in
            );
            // stored.url() là public URL do S3StorageServiceImpl.buildPublicUrl(key) sinh
            return new PutResult(stored.key(), stored.url());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public PutResult putObject(String folder, String filename, MultipartFile file) {
        // Tương thích ngược: gộp thành key "folder/filename"
        String key = (folder.endsWith("/") ? folder : folder + "/") + filename;
        return putObject(key, file);
    }

    @Override
    public byte[] getObject(String key) {
        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build()
            );
            return bytes.asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read object from S3: " + key, e);
        }
    }
}
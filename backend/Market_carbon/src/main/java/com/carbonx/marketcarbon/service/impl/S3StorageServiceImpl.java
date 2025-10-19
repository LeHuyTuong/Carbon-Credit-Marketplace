package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.public-base-url}")
    private String publicBaseUrl;

    @Override
    public StoredObject upload(String key, String contentType, long contentLength, InputStream in) {
        log.info("Uploading to S3: bucket={}, key={}, contentType={}, length={}", bucket, key, contentType, contentLength);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        try (in) {
            PutObjectResponse resp = s3.putObject(req, RequestBody.fromInputStream(in, contentLength));

            String url = buildPublicUrl(key);
            log.info("Uploaded successfully. ETag={}, URL={}", resp.eTag(), url);

            return new StoredObject(key, resp.eTag(), url);
        } catch (IOException ex) {
            log.error("Upload failed: {}", ex.getMessage(), ex);
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String buildPublicUrl(String key) {
        String url = publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
        log.info("Built public URL: {}", url);
        return url;
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            log.warn("Delete called with empty key, skipping");
            return;
        }
        log.info("Deleting from S3: bucket={}, key={}", bucket, key);
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}

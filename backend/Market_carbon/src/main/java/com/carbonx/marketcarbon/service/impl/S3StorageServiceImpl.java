package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.StorageService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3;
    @Value("${aws.s3.bucket}") private String bucket;
    @Value("${aws.s3.public-base-url}") private String publicBaseUrl;

    @Override
    public StoredObject upload(String key, String contentType, long contentLength, InputStream in) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                // .acl(ObjectCannedACL.PUBLIC_READ) // KHÔNG khuyến nghị; dùng CloudFront hoặc presigned URL tốt hơn
                .build();

        try (in) {
            PutObjectResponse resp = s3.putObject(
                    req,
                    RequestBody.fromInputStream(in, contentLength)
            );
            String url = buildPublicUrl(key);
            return new StoredObject(key, resp.eTag(), url);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String buildPublicUrl(String key) {
        // Nếu dùng CloudFront, thay publicBaseUrl bằng domain CloudFront
        return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}

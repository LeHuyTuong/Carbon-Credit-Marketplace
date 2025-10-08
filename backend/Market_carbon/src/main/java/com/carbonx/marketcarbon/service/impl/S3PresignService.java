package com.carbonx.marketcarbon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignService {
    private final S3Presigner presigner;
    @Value("${aws.s3.bucket}") private String bucket;

    public URL createPutUrl(String key, String contentType, Duration ttl) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(b -> b
                .signatureDuration(ttl)
                .putObjectRequest(putReq));
        return presigned.url();
    }
}

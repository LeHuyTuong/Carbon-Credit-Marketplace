package com.carbonx.marketcarbon.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    record PutResult(String key, String url) {}
    PutResult putObject(String folder, String objectName, MultipartFile file);
    byte[] getObject(String key);
}

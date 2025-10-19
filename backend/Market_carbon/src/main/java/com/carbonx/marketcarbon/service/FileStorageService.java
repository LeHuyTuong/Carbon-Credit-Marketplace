package com.carbonx.marketcarbon.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    record PutResult(String key, String url) {}

    // Overload mới: truyền vào KEY đầy đủ
    PutResult putObject(String key, MultipartFile file);

    // Overload cũ: giữ lại cho tương thích, sẽ gộp folder/filename thành key
    PutResult putObject(String folder, String filename, MultipartFile file);

    byte[] getObject(String key);
}

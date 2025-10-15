package com.carbonx.marketcarbon.service;

import java.io.InputStream;

public interface StorageService {
    StoredObject upload(String key, String contentType, long contentLength, InputStream in);
    String buildPublicUrl(String key);
    record StoredObject(String key, String eTag, String url) {}
    void delete(String key);
}

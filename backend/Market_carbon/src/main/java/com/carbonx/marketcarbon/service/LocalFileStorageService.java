package com.carbonx.marketcarbon.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
@Profile("local") // dùng profile để tránh đụng bean khi có S3; hoặc bỏ nếu bạn dùng @Qualifier
public class LocalFileStorageService implements FileStorageService {

    @Value("${storage.local.base-dir:uploads}")
    private String baseDir;

    @Value("${storage.local.public-base-url:http://localhost:8082/files}")
    private String publicBaseUrl;

    private Path root;

    @PostConstruct
    void init() {
        root = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("Local storage root: {}", root);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create local storage root: " + root, e);
        }
    }

    @Override
    public PutResult putObject(String key, MultipartFile file) {
        try {
            String safeKey = sanitizeKey(key);
            Path path = root.resolve(safeKey).normalize();
            if (!path.startsWith(root)) {
                throw new SecurityException("Invalid key path traversal: " + key);
            }
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            String url = buildPublicUrl(safeKey);
            log.info("Stored locally: key={}, path={}, url={}", safeKey, path, url);
            return new PutResult(safeKey, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file (key=" + key + ")", e);
        }
    }

    @Override
    public PutResult putObject(String folder, String objectName, MultipartFile file) {
        String safeName = System.currentTimeMillis() + "_" + objectName.replaceAll("\\s+", "_");
        String key = (folder.endsWith("/") ? folder : folder + "/") + safeName;
        return putObject(key, file);
    }

    @Override
    public byte[] getObject(String key) {
        try {
            String safeKey = sanitizeKey(key);
            Path path = root.resolve(safeKey).normalize();
            if (!path.startsWith(root)) {
                throw new SecurityException("Invalid key path traversal: " + key);
            }
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + key, e);
        }
    }

    private String buildPublicUrl(String key) {
        return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
    }

    private String sanitizeKey(String key) {
        String k = key.replace("\\", "/");
        while (k.startsWith("/")) k = k.substring(1);
        if (k.contains("..")) throw new IllegalArgumentException("Key cannot contain '..'");
        return k;
    }
}

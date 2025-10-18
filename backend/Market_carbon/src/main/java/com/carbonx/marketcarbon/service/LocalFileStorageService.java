package com.carbonx.marketcarbon.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService implements FileStorageService {
    private static final Path ROOT = Paths.get("uploads");

    @Override
    public PutResult putObject(String folder, String objectName, MultipartFile file) {
        try {
            Path dir = ROOT.resolve(folder);
            Files.createDirectories(dir);
            String safeName = System.currentTimeMillis() + "_" + objectName.replaceAll("\\s+", "_");
            String key = folder + "/" + safeName;
            Path path = ROOT.resolve(key);
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            // Không cần URL thì để null
            return new PutResult(key, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public byte[] getObject(String key) {
        try {
            return Files.readAllBytes(ROOT.resolve(key));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + key, e);
        }
    }
}
package com.carbonx.marketcarbon.certificate;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ImageBase64Cache {

    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    public String get(String url) {
        if (url == null || url.isBlank()) return null;
        return cache.computeIfAbsent(url, this::loadBase64);
    }

    private String loadBase64(String u) {
        try {
            URLConnection conn = new URL(u).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream in = conn.getInputStream()) {
                byte[] b = in.readAllBytes();
                String mime = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(b));
                if (mime == null) mime = u.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(b);
            }
        } catch (Exception e) {
            return null;
        }
    }
}

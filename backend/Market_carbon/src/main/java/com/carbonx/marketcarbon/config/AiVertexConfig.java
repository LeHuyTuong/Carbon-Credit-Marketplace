package com.carbonx.marketcarbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.vertex")
public class AiVertexConfig {

    private String projectId;


    private String location = "us-central1";


    private String model = "gemini-2.5-pro";


    private boolean enabled = true;


    private int timeoutMs = 60000;

    /**
     * Đường dẫn file khoá dịch vụ Google (JSON). Rỗng = không dùng Vertex.
     * Trước đây đường dẫn này bị hardcode trong code, nên prod không có file đó là
     * không boot được.
     */
    private String credentialsFile = "";
}

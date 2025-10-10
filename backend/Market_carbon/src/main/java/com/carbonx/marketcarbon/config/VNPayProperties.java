package com.carbonx.marketcarbon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayProperties {
    private String payUrl;
    private String apiUrl;
    private String returnUrl;
    private String tmnCode;
    private String hashSecret;
}

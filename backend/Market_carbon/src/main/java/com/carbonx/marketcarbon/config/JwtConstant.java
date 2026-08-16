package com.carbonx.marketcarbon.config;

public class JwtConstant {
    // P0-A (N4): signing secret removed from source — it was publicly readable and
    // allowed forging tokens for any account. Configure via `jwt.secret` property /
    // JWT_SECRET env var (see application.properties and application-prod.properties).
    public static final String JWT_HEADER = "Authorization";
}

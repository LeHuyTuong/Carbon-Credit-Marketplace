package com.carbonx.marketcarbon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI custoOpenAPI(@Value("API document") String title,
                                @Value("1.0.0") String version,
                                @Value("Mo ta API Service") String description,
                                @Value("http://localhost:8082") String serverURL,
                                @Value("server test") String serverName)
    {
        return new OpenAPI().info(new Info().title(title)
                .version(version)
                .description(description)
                .license(new License().name("API License")))
                .servers(List.of(new Server().url(serverURL).description(serverName)));
    }

    @Bean
    public GroupedOpenApi customOpenApi() {
        return GroupedOpenApi.builder()
                .group("api-service-1")
                .packagesToScan("com.carbonx.marketcarbon.controller")
                .build();
    }
}

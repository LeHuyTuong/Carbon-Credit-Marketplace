package com.carbonx.marketcarbon.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",                 // tên scheme (dùng trong controller nếu cần)
        type = SecuritySchemeType.HTTP,      // kiểu HTTP
        scheme = "bearer",                   // định nghĩa Bearer
        bearerFormat = "JWT"                 // format JWT
)
public class OpenApiConfig {

    @Bean
    public OpenAPI custoOpenAPI(@Value("${springdoc.title}") String title,
                                @Value("${springdoc.version}") String version,
                                @Value("${springdoc.description}") String description,
                                @Value("${server.url}") String serverURL,
                                @Value("${server.description}") String serverName)
    {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title(title)
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

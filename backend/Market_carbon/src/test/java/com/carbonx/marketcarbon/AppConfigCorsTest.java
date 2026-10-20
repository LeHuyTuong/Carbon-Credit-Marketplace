package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.AppConfig;
import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.config.JwtTokenValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * H3: CORS origins and the OAuth2 frontend redirect must come from configuration
 * ({@code app.cors.allowed-origins} / {@code app.frontend.base-url}) instead of
 * hardcoded values. The list must be parsed and trimmed, credentials must stay
 * enabled, and AppConfig source must no longer branch on the request host.
 */
class AppConfigCorsTest {

    @Test
    void corsAllowedOrigins_areParsedFromConfiguration_andTrimmed() throws Exception {
        AppConfig appConfig = newAppConfig();
        ReflectionTestUtils.setField(appConfig, "corsAllowedOrigins",
                "http://localhost:3000, http://localhost:5173 ,https://example.test");

        CorsConfiguration cfg = corsConfiguration(appConfig);

        assertThat(cfg.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173", "https://example.test");
        assertThat(cfg.getAllowCredentials()).isTrue();
        assertThat(cfg.getAllowedMethods()).containsExactly("*");
        assertThat(cfg.getAllowedHeaders()).containsExactly("*");
        assertThat(cfg.getExposedHeaders()).containsExactly("Authorization");
        assertThat(cfg.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsAllowedOrigins_supportsSingleOrigin() throws Exception {
        AppConfig appConfig = newAppConfig();
        ReflectionTestUtils.setField(appConfig, "corsAllowedOrigins", "https://carbonx.io.vn");

        CorsConfiguration cfg = corsConfiguration(appConfig);

        assertThat(cfg.getAllowedOrigins()).containsExactly("https://carbonx.io.vn");
        assertThat(cfg.getAllowCredentials()).isTrue();
    }

    @Test
    void appConfigSource_noLongerBranchesOnHardcodedHosts() {
        // Contract guard (source-file assertion): the OAuth2 redirect target must be
        // driven by the injected frontendBaseUrl field, and the CORS domains must not
        // remain as literals in code.
        String source = readAppConfigSource();
        assertThat(source)
                .as("OAuth2 redirect must not branch on the request host")
                .doesNotContain("getServerName");
        assertThat(source)
                .as("production frontend domain must not be hardcoded")
                .doesNotContain("carbonx.io.vn");
        assertThat(source)
                .as("dev frontend origin must not be hardcoded")
                .doesNotContain("localhost:5173");
        assertThat(source)
                .as("redirect must use the injected frontendBaseUrl field")
                .contains("sendRedirect(frontendBaseUrl");
    }

    private AppConfig newAppConfig() {
        return new AppConfig(mock(JwtTokenValidator.class), mock(JwtProvider.class));
    }

    private CorsConfiguration corsConfiguration(AppConfig appConfig) throws Exception {
        Method method = AppConfig.class.getDeclaredMethod("corsConfigurationSource");
        method.setAccessible(true);
        CorsConfigurationSource source = (CorsConfigurationSource) method.invoke(appConfig);
        return source.getCorsConfiguration(mock(HttpServletRequest.class));
    }

    private String readAppConfigSource() {
        Path path = Paths.get("src/main/java/com/carbonx/marketcarbon/config/AppConfig.java");
        if (!Files.exists(path)) {
            path = Paths.get("backend/Market_carbon/src/main/java/com/carbonx/marketcarbon/config/AppConfig.java");
        }
        final Path resolved = path;
        assertThat(Files.exists(resolved))
                .as("AppConfig.java must be readable from the test working directory (%s)", resolved.toAbsolutePath())
                .isTrue();
        try {
            return Files.readString(resolved);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

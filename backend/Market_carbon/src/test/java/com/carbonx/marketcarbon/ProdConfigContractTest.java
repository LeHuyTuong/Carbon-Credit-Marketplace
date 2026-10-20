package com.carbonx.marketcarbon;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for hardcode findings H1–H4: the production profile must not
 * mutate the schema on boot (H1), must not pin PayPal to sandbox (H2), and the
 * frontend redirect / CORS / local-storage values must be configuration
 * properties with env-overridable defaults (H3, H4).
 */
class ProdConfigContractTest {

    @Test
    void prodProfile_ddlAutoDefaultsToValidate() {
        String prod = readClasspathFile("application-prod.properties");
        assertThat(lineStartingWith(prod, "spring.jpa.hibernate.ddl-auto"))
                .as("H1: production must default to schema validation, never mutation")
                .isEqualTo("spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}");
        assertThat(prod).doesNotContain("${JPA_DDL_AUTO:update}");
    }

    @Test
    void prodProfile_doesNotPinPaypalModeToSandbox() {
        String prod = readClasspathFile("application-prod.properties");
        assertThat(prod)
                .as("H2: prod must not force paypal sandbox mode")
                .doesNotContain("paypal.mode=sandbox");
        assertThat(activeLines(prod))
                .as("H2: prod must not declare paypal.mode — the base default applies")
                .noneMatch(line -> line.startsWith("paypal.mode"));

        String base = readClasspathFile("application.properties");
        assertThat(lineStartingWith(base, "paypal.mode"))
                .as("H2: base paypal.mode must fail fast when PAYPAL_MODE is missing (no fallback)")
                .isEqualTo("paypal.mode=${PAYPAL_MODE}");
    }

    @Test
    void prodProfile_definesFrontendAndCorsWithEnvPlaceholders() {
        String prod = readClasspathFile("application-prod.properties");
        assertThat(lineStartingWith(prod, "app.frontend.base-url"))
                .isEqualTo("app.frontend.base-url=${APP_FRONTEND_BASE_URL:https://carbonx.io.vn}");
        assertThat(lineStartingWith(prod, "app.cors.allowed-origins"))
                .isEqualTo("app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:https://carbonx.io.vn}");
    }

    @Test
    void baseProfile_definesFrontendCorsAndStorageWithEnvPlaceholders() {
        String base = readClasspathFile("application.properties");
        assertThat(lineStartingWith(base, "app.frontend.base-url"))
                .isEqualTo("app.frontend.base-url=${APP_FRONTEND_BASE_URL:http://localhost:5173}");
        assertThat(lineStartingWith(base, "app.cors.allowed-origins"))
                .isEqualTo("app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}");
        assertThat(lineStartingWith(base, "storage.local.public-base-url"))
                .isEqualTo("storage.local.public-base-url=${STORAGE_LOCAL_PUBLIC_BASE_URL:http://localhost:8082/files}");
        assertThat(lineStartingWith(base, "storage.local.base-dir"))
                .isEqualTo("storage.local.base-dir=${STORAGE_LOCAL_BASE_DIR:uploads}");
    }

    private String lineStartingWith(String props, String prefix) {
        for (String rawLine : props.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        throw new AssertionError("No property line starting with '" + prefix + "' found");
    }

    /** Property lines only — comments/blanks are documentation, not configuration. */
    private java.util.List<String> activeLines(String props) {
        return props.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("!"))
                .toList();
    }

    private String readClasspathFile(String name) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(in).as("classpath resource %s", name).isNotNull();
            try (Scanner sc = new Scanner(in, StandardCharsets.UTF_8)) {
                return sc.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

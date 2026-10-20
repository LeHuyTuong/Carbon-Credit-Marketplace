package com.carbonx.marketcarbon;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertNoDuplicateKeys(prod, "spring.jpa.hibernate.ddl-auto");
        assertThat(lineStartingWith(prod, "spring.jpa.hibernate.ddl-auto"))
                .as("H1: production must default to schema validation, never mutation")
                .isEqualTo("spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}");
        assertThat(prod).doesNotContain("${JPA_DDL_AUTO:update}");

        String base = readClasspathFile("application.properties");
        assertNoDuplicateKeys(base, "spring.jpa.hibernate.ddl-auto");
    }

    @Test
    void prodProfile_doesNotPinPaypalModeToSandbox() {
        String prod = readClasspathFile("application-prod.properties");
        assertNoDuplicateKeys(prod, "paypal.mode");
        assertThat(prod)
                .as("H2: prod must not force paypal sandbox mode")
                .doesNotContain("paypal.mode=sandbox");
        assertThat(activeLines(prod))
                .as("H2: prod must not declare paypal.mode — the base default applies")
                .noneMatch(line -> line.startsWith("paypal.mode"));

        String base = readClasspathFile("application.properties");
        assertNoDuplicateKeys(base, "paypal.mode");
        assertThat(lineStartingWith(base, "paypal.mode"))
                .as("H2: base paypal.mode must fail fast when PAYPAL_MODE is missing (no fallback)")
                .isEqualTo("paypal.mode=${PAYPAL_MODE}");
    }

    @Test
    void prodProfile_definesFrontendAndCorsWithEnvPlaceholders() {
        String prod = readClasspathFile("application-prod.properties");
        assertNoDuplicateKeys(prod, "app.frontend.base-url", "app.cors.allowed-origins");
        assertThat(lineStartingWith(prod, "app.frontend.base-url"))
                .isEqualTo("app.frontend.base-url=${APP_FRONTEND_BASE_URL:https://carbonx.io.vn}");
        assertThat(lineStartingWith(prod, "app.cors.allowed-origins"))
                .isEqualTo("app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:https://carbonx.io.vn}");
    }

    @Test
    void baseProfile_definesFrontendCorsAndStorageWithEnvPlaceholders() {
        String base = readClasspathFile("application.properties");
        assertNoDuplicateKeys(base, "app.frontend.base-url", "app.cors.allowed-origins",
                "storage.local.base-dir", "storage.local.public-base-url");
        assertThat(lineStartingWith(base, "app.frontend.base-url"))
                .isEqualTo("app.frontend.base-url=${APP_FRONTEND_BASE_URL:http://localhost:5173}");
        assertThat(lineStartingWith(base, "app.cors.allowed-origins"))
                .isEqualTo("app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}");
        assertThat(lineStartingWith(base, "storage.local.public-base-url"))
                .isEqualTo("storage.local.public-base-url=${STORAGE_LOCAL_PUBLIC_BASE_URL:http://localhost:8082/files}");
        assertThat(lineStartingWith(base, "storage.local.base-dir"))
                .isEqualTo("storage.local.base-dir=${STORAGE_LOCAL_BASE_DIR:uploads}");
    }

    @Test
    void everyInjectedPlaceholderIsDefinedInSomeProfile() throws Exception {
        // A @Value("${key}") whose key is defined in NO properties file aborts the boot:
        // "Could not resolve placeholder ...". Nothing else in the suite caught that, so
        // the app could not start with any profile.
        java.util.Set<String> defined = new java.util.HashSet<>();
        defined.addAll(activeKeys(readClasspathFile("application.properties")));
        defined.addAll(activeKeys(readClasspathFile("application-prod.properties")));

        java.util.List<String> missing = new java.util.ArrayList<>();
        java.nio.file.Path sources = java.nio.file.Path.of("src", "main", "java");
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(sources)) {
            for (java.nio.file.Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = java.nio.file.Files.readString(file);
                java.util.regex.Matcher matcher =
                        java.util.regex.Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)(:[^}]*)?\\}").matcher(content);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    boolean hasDefaultInCode = matcher.group(2) != null;
                    // A placeholder with a default in the code still resolves; one without
                    // any default aborts the boot when no properties file defines it.
                    if (!hasDefaultInCode && key.contains(".") && !defined.contains(key)) {
                        missing.add(file.getFileName() + " -> " + key);
                    }
                }
            }
        }
        assertThat(missing)
                .as("every @Value placeholder must exist in application.properties or application-prod.properties")
                .isEmpty();
    }

    private java.util.Set<String> activeKeys(String props) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (String line : activeLines(props)) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                keys.add(line.substring(0, separator).trim());
            }
        }
        return keys;
    }

    @Test
    void lineStartingWith_returnsLastActiveValueBecausePropertiesLoadIsLastWins() {
        String props = String.join("\n",
                "# paypal.mode=commented-out",
                "paypal.mode=first",
                "paypal.mode=last");

        assertThat(lineStartingWith(props, "paypal.mode"))
                .as("a duplicate appended by a merge must not be masked by the first line")
                .isEqualTo("paypal.mode=last");
    }

    @Test
    void duplicateKeysAreRejectedAsMergeHazard() {
        String props = String.join("\n",
                "paypal.mode=${PAYPAL_MODE}",
                "paypal.mode=sandbox");

        assertThatThrownBy(() -> assertNoDuplicateKeys(props, "paypal.mode"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("paypal.mode");
    }

    private String lineStartingWith(String props, String prefix) {
        String match = null;
        for (String line : activeLines(props)) {
            if (line.startsWith(prefix + "=") || line.startsWith(prefix + ":")) {
                match = line;
            }
        }
        if (match == null) {
            throw new AssertionError("No property line starting with '" + prefix + "' found");
        }
        return match;
    }

    /**
     * Duplicate keys are a merge hazard: {@link java.util.Properties#load} is last-wins,
     * so the first line can look correct while the effective value comes from the last.
     */
    private void assertNoDuplicateKeys(String props, String... keys) {
        List<String> lines = activeLines(props);
        for (String key : keys) {
            List<String> matches = lines.stream()
                    .filter(line -> line.startsWith(key + "=") || line.startsWith(key + ":"))
                    .toList();
            assertThat(matches)
                    .as("duplicate property '%s' is a merge hazard (Properties.load is last-wins)", key)
                    .hasSizeLessThanOrEqualTo(1);
        }
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

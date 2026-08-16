package com.carbonx.marketcarbon;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-A (N4): contract test — the production profile must resolve jwt.secret from
 * the JWT_SECRET environment variable with NO fallback, so that a production boot
 * without the secret fails fast instead of silently using a dev default.
 */
class ProdJwtSecretContractTest {

    @Test
    void prodProfile_jwtSecretHasNoFallback() {
        String props = readClasspathFile("application-prod.properties");
        assertThat(props).contains("jwt.secret=${JWT_SECRET}");
        for (String rawLine : props.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("jwt.secret")) {
                // a default would appear as ${JWT_SECRET:fallback}
                int close = line.indexOf('}');
                int colon = line.indexOf(':', line.indexOf("${") + 2);
                assertThat(colon == -1 || colon > close)
                        .as("jwt.secret must not declare a fallback default: %s", line)
                        .isTrue();
            }
        }
    }

    @Test
    void prodProfile_containsNoPlaintextCredentialValues() {
        String props = readClasspathFile("application-prod.properties");
        // every sensitive property must be a ${ENV_VAR} placeholder, never a literal
        for (String rawLine : props.split("\n")) {
            String line = rawLine.trim();
            String lower = line.toLowerCase();
            if ((lower.contains("password") || lower.contains("secret") || lower.contains("accesskey"))
                    && line.contains("=")) {
                assertThat(line.matches("[^=]+=\\$\\{[A-Z0-9_]+}.*"))
                        .as("sensitive property must use an env placeholder: %s", line)
                        .isTrue();
            }
        }
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

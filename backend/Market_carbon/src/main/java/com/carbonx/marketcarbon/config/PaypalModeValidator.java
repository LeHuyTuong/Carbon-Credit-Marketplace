package com.carbonx.marketcarbon.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Fails production startup when {@code paypal.mode} is missing or invalid.
 *
 * <p>An empty {@code PAYPAL_MODE} resolves to an empty string at boot; without this
 * check the application starts normally and only fails on the first PayPal request
 * with {@code IllegalArgumentException: Mode needs to be either 'sandbox' or 'live'}.</p>
 */
@Component
@Profile("prod")
public class PaypalModeValidator {

    static final Set<String> ALLOWED_MODES = Set.of("sandbox", "live");

    private final String mode;

    public PaypalModeValidator(@Value("${paypal.mode:}") String mode) {
        this.mode = mode;
    }

    @PostConstruct
    void validateMode() {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MODES.contains(normalized)) {
            throw new IllegalStateException(String.format(
                    "Invalid paypal.mode='%s'. Set PAYPAL_MODE to 'sandbox' or 'live' in production; "
                            + "blank or unsupported values are rejected at startup.",
                    mode == null ? "" : mode));
        }
    }
}

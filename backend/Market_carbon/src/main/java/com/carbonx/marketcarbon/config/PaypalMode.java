package com.carbonx.marketcarbon.config;

import java.util.Locale;

/**
 * Normalizes {@code paypal.mode} to the exact strings expected by the PayPal REST SDK.
 *
 * <p>The SDK's {@code APIContext} compares the mode against {@code Constants.LIVE} and
 * {@code Constants.SANDBOX} with {@code equals}, so {@code Live} or {@code " sandbox "} is
 * rejected at request time. Normalizing once here keeps startup validation and both SDK call
 * sites in agreement.</p>
 */
public final class PaypalMode {

    private PaypalMode() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

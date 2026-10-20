package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.PaypalMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RV-2-02: the PayPal SDK only accepts the exact strings {@code sandbox} and {@code live}.
 * Startup validation already normalized the value, but both {@code APIContext} call sites passed
 * the raw property, so {@code PAYPAL_MODE=Live} booted and then failed on the first request.
 */
class PaypalModeTest {

    @Test
    void nullAndBlankBecomeEmpty() {
        assertThat(PaypalMode.normalize(null)).isEmpty();
        assertThat(PaypalMode.normalize("")).isEmpty();
        assertThat(PaypalMode.normalize("   ")).isEmpty();
    }

    @Test
    void caseAndSurroundingWhitespaceAreNormalizedForTheSdk() {
        assertThat(PaypalMode.normalize("Live")).isEqualTo("live");
        assertThat(PaypalMode.normalize(" LIVE ")).isEqualTo("live");
        assertThat(PaypalMode.normalize("Sandbox")).isEqualTo("sandbox");
    }

    @Test
    void unsupportedValuesStayNormalizedButAreStillRejectedByTheValidator() {
        assertThat(PaypalMode.normalize("production")).isEqualTo("production");
    }

    @Test
    void utilityClassIsFinalAndNotInstantiable() throws Exception {
        Constructor<PaypalMode> constructor = PaypalMode.class.getDeclaredConstructor();

        assertThat(Modifier.isFinal(PaypalMode.class.getModifiers())).isTrue();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }
}

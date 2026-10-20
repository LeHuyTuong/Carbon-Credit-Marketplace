package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.PaypalModeValidator;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * H2: an empty/blank/invalid PAYPAL_MODE must fail at startup in production instead of
 * booting and throwing {@code IllegalArgumentException} on the first PayPal request.
 */
class PaypalModeValidatorTest {

    @Test
    void blankOrInvalidModeFailsFast() {
        for (String mode : Arrays.asList(null, "", "   ", "production", "sandboxx")) {
            assertThatThrownBy(() -> validate(mode))
                    .as("paypal.mode=%s must fail startup", mode)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("paypal.mode")
                    .hasMessageContaining("sandbox")
                    .hasMessageContaining("live");
        }
    }

    @Test
    void supportedModesPassInAnyCase() {
        for (String mode : List.of("sandbox", "live", "LIVE", " Sandbox ")) {
            assertThatCode(() -> validate(mode))
                    .as("paypal.mode=%s must be accepted", mode)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void validatorRunsOnlyInProdViaPostConstruct() throws Exception {
        assertThat(PaypalModeValidator.class.getAnnotation(Component.class))
                .as("startup validation must be a component")
                .isNotNull();
        assertThat(PaypalModeValidator.class.getAnnotation(Profile.class).value())
                .as("only the VALIDATOR is prod-only: the paypal.mode property itself has no "
                        + "fallback in the base profile, so every profile needs PAYPAL_MODE set")
                .containsExactly("prod");

        Method method = PaypalModeValidator.class.getDeclaredMethod("validateMode");
        assertThat(method.getAnnotation(PostConstruct.class))
                .as("validation must run during bean initialization")
                .isNotNull();
    }

    private void validate(String mode) {
        ReflectionTestUtils.invokeMethod(new PaypalModeValidator(mode), "validateMode");
    }
}

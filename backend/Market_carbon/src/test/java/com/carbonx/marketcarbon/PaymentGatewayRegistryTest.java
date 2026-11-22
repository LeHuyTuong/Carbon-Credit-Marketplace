package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.service.payment.gateway.PaypalPaymentGateway;
import com.carbonx.marketcarbon.service.payment.gateway.PaymentGateway;
import com.carbonx.marketcarbon.service.payment.gateway.PaymentGatewayRegistry;
import com.carbonx.marketcarbon.service.payment.gateway.StripePaymentGateway;
import com.carbonx.marketcarbon.service.payment.gateway.VnpayPaymentGateway;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayRegistryTest {

    private final PaymentGateway stripe = new StripePaymentGateway();
    private final PaymentGateway paypal = new PaypalPaymentGateway();
    private final PaymentGateway vnpay = new VnpayPaymentGateway();
    private final PaymentGatewayRegistry registry =
            new PaymentGatewayRegistry(List.of(stripe, paypal, vnpay));

    @Test
    void resolvesEverySupportedMethod() {
        assertThat(registry.resolve(PaymentMethod.STRIPE)).isSameAs(stripe);
        assertThat(registry.resolve(PaymentMethod.PAYPAL)).isSameAs(paypal);
        assertThat(registry.resolve(PaymentMethod.VNPAY)).isSameAs(vnpay);
    }

    @Test
    void eachGatewayDeclaresItsOwnMethod() {
        assertThat(stripe.supportedMethod()).isEqualTo(PaymentMethod.STRIPE);
        assertThat(paypal.supportedMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(vnpay.supportedMethod()).isEqualTo(PaymentMethod.VNPAY);
    }

    @Test
    void defaultsRegistryResolvesAllMethods() {
        PaymentGatewayRegistry defaults = PaymentGatewayRegistry.withDefaults();

        for (PaymentMethod method : PaymentMethod.values()) {
            assertThat(defaults.resolve(method).supportedMethod()).isEqualTo(method);
        }
    }

    @Test
    void firstGatewayWinsOnDuplicateMethod() {
        PaymentGateway duplicateStripe = new PaymentGateway() {
            @Override public PaymentMethod supportedMethod() { return PaymentMethod.STRIPE; }

            @Override public com.carbonx.marketcarbon.dto.response.PaymentOrderResponse createPayment(
                    com.carbonx.marketcarbon.model.User user, Long amount, Long orderId) {
                return null;
            }
        };

        PaymentGatewayRegistry withDuplicate =
                new PaymentGatewayRegistry(Arrays.asList(duplicateStripe, stripe));

        assertThat(withDuplicate.resolve(PaymentMethod.STRIPE)).isSameAs(duplicateStripe);
    }

    @Test
    void nullAndEmptyRegistriesRejectResolution() {
        assertThatThrownBy(() -> new PaymentGatewayRegistry(null).resolve(PaymentMethod.STRIPE))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> new PaymentGatewayRegistry(Collections.emptyList()).resolve(PaymentMethod.PAYPAL))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> registry.resolve(null)).isInstanceOf(AppException.class);
    }

    @Test
    void nullEntriesAreIgnored() {
        PaymentGatewayRegistry withNulls = new PaymentGatewayRegistry(Arrays.asList(null, stripe));

        assertThat(withNulls.resolve(PaymentMethod.STRIPE)).isSameAs(stripe);
    }
}

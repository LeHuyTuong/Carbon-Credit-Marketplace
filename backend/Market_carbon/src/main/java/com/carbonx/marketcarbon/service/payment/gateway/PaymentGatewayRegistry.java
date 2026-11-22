package com.carbonx.marketcarbon.service.payment.gateway;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Chọn {@link PaymentGateway} theo {@link PaymentMethod}. */
@Component
public class PaymentGatewayRegistry {

    private final Map<PaymentMethod, PaymentGateway> gatewaysByMethod;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        Map<PaymentMethod, PaymentGateway> byMethod = new EnumMap<>(PaymentMethod.class);
        if (gateways != null) {
            for (PaymentGateway gateway : gateways) {
                if (gateway != null && gateway.supportedMethod() != null) {
                    byMethod.putIfAbsent(gateway.supportedMethod(), gateway);
                }
            }
        }
        this.gatewaysByMethod = byMethod;
    }

    public PaymentGateway resolve(PaymentMethod method) {
        PaymentGateway gateway = method == null ? null : gatewaysByMethod.get(method);
        if (gateway == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        return gateway;
    }

    /** Dùng khi registry không được inject (ví dụ unit test khởi tạo service bằng tay). */
    public static PaymentGatewayRegistry withDefaults() {
        return new PaymentGatewayRegistry(List.of(
                new StripePaymentGateway(), new PaypalPaymentGateway(), new VnpayPaymentGateway()));
    }
}

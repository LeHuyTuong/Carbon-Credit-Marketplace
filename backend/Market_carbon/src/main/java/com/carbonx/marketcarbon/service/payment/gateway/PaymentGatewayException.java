package com.carbonx.marketcarbon.service.payment.gateway;

/** Lỗi gọi provider thanh toán; giữ nguyên nguyên nhân gốc để service có thể rethrow. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}

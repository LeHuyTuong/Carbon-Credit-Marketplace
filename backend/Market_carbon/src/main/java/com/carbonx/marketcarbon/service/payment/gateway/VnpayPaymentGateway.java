package com.carbonx.marketcarbon.service.payment.gateway;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.User;
import org.springframework.stereotype.Component;

/**
 * Chiến lược VNPay: việc tạo URL thanh toán nằm ở {@code VNPayService} (cần HttpServletRequest
 * để lấy IP và ký HMAC), còn đơn nạp chỉ được credit qua callback đã kiểm chữ ký.
 * Gateway này chỉ đại diện cho phương thức trong registry; không mở một đường credit mới.
 */
@Component
public class VnpayPaymentGateway implements PaymentGateway {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public PaymentOrderResponse createPayment(User user, Long amount, Long orderId) {
        throw new PaymentGatewayException(
                "VNPay payment URL must be created by VNPayService and confirmed by its signed callback");
    }
}

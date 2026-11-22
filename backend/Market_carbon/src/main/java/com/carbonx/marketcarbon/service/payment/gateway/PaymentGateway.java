package com.carbonx.marketcarbon.service.payment.gateway;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.User;

/**
 * Strategy tạo link thanh toán cho một phương thức thanh toán.
 *
 * <p>Mỗi implementation chỉ biết cách gọi provider của nó; {@link PaymentGatewayRegistry}
 * chọn implementation theo {@link PaymentMethod}.</p>
 */
public interface PaymentGateway {

    PaymentMethod supportedMethod();

    /**
     * @param user    chủ sở hữu đơn nạp tiền (để lại tham số cho provider cần định danh)
     * @param amount  số tiền theo đơn vị của provider (USD với Stripe/PayPal, VND với VNPay)
     * @param orderId id đơn thanh toán đã được lưu ở bước createOrder
     * @return response chứa payment_url/id/amount
     */
    PaymentOrderResponse createPayment(User user, Long amount, Long orderId);
}

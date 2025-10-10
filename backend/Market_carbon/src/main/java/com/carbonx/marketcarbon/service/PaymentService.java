package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.stripe.exception.StripeException;

public interface PaymentService {
    PaymentOrderResponse createOrder(PaymentOrderRequest request);
    PaymentOrder getPaymentOrderById(Long id);
    Boolean processPaymentOrder(PaymentOrder paymentOrder, String paymentId) ;
    PaymentOrderResponse createStripePaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws StripeException;
}

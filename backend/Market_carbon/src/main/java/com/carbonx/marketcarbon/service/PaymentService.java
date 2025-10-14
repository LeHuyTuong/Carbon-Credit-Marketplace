package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.stripe.exception.StripeException;

import java.util.List;

public interface PaymentService {
    PaymentOrderResponse createOrder(PaymentOrderRequest request);
    List<PaymentOrder> getAllPaymentByUser();
    PaymentOrder getPaymentOrderById(Long id);
    Boolean processPaymentOrder(PaymentOrder paymentOrder, String paymentId) ;
    PaymentOrderResponse createStripePaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws StripeException;
}

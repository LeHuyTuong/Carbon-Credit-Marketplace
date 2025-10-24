package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.paypal.base.rest.PayPalRESTException;
import com.stripe.exception.StripeException;

import java.util.List;

public interface PaymentService {
    PaymentOrderResponse createOrder(PaymentOrderRequest request);
    List<PaymentOrder> getAllPaymentByUser();
    PaymentOrder getPaymentOrderById(Long id);
    Boolean processPaymentOrder(Long orderId, String paymentId) ;
    PaymentOrderResponse createStripePaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws StripeException;
    PaymentOrderResponse createPayPalPaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws PayPalRESTException;

    PaymentOrder createOrderVNPay(Long amount, String orderInfo, String vnp_TxnRef);
    void updateOrderStatus(String vnp_TxnRef, Status status);
}

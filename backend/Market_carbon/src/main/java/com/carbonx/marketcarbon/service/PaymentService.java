package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.Wallet;
import com.paypal.base.rest.PayPalRESTException;
import com.stripe.exception.StripeException;

import java.util.List;

public interface PaymentService {
    PaymentOrderResponse createOrder(PaymentOrderRequest request);
    List<PaymentOrder> getAllPaymentByUser();
    PaymentOrder getPaymentOrderById(Long id);

    // P0-B/B1: server-side deposit confirmation
    /** Ownership check + provider-side verification (external HTTP, NOT transactional). Throws on any failure. */
    void assertDepositVerifiable(Long orderId);

    /** Transactional exactly-once credit: locks the order, guards PENDING → SUCCEEDED, credits wallet + ledger atomically. */
    Wallet applyVerifiedDeposit(Long orderId);

    /** Transactional exactly-once credit for a VNPay order already verified by HMAC signature at the return URL. */
    boolean applyVnPayDeposit(String vnpTxnRef);

    PaymentOrderResponse createStripePaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws StripeException;
    PaymentOrderResponse createPayPalPaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws PayPalRESTException;

    PaymentOrder createOrderVNPay(Long amount, String orderInfo, String vnp_TxnRef);
    void updateOrderStatus(String vnp_TxnRef, Status status);
}

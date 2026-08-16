package com.carbonx.marketcarbon.service;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.APIContext;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * P0-B/B1: server-side payment verification against the provider APIs.
 *
 * Trust boundary: a payment may only be credited after the BACKEND has confirmed
 * with the provider (Stripe/PayPal) that the referenced payment exists, is in a
 * paid/approved state, and matches the expected amount. Frontend redirect pages
 * and client-supplied ids are outside the trust boundary.
 *
 * This bean isolates the provider SDK calls so the deposit orchestration
 * (PaymentServiceImpl) stays unit-testable and no external HTTP call ever happens
 * inside a financial DB transaction.
 */
@Slf4j
@Service
public class PaymentVerificationService {

    private final String stripeKey;
    private final String paypalClientId;
    private final String paypalClientSecret;
    private final String paypalMode;

    public PaymentVerificationService(
            @Value("${stripe.api.key}") String stripeKey,
            @Value("${paypal.client.id}") String paypalClientId,
            @Value("${paypal.client.secret}") String paypalClientSecret,
            @Value("${paypal.mode}") String paypalMode) {
        this.stripeKey = stripeKey;
        this.paypalClientId = paypalClientId;
        this.paypalClientSecret = paypalClientSecret;
        this.paypalMode = paypalMode;
    }

    /**
     * Verify a Stripe Checkout Session is paid and matches the expected USD amount.
     * @param sessionId   provider reference stored server-side at link creation
     * @param expectedUsd the PaymentOrder amount in whole USD
     */
    public boolean verifyStripePaid(String sessionId, Long expectedUsd) {
        try {
            Stripe.apiKey = stripeKey;
            Session session = Session.retrieve(sessionId);
            boolean paid = "paid".equals(session.getPaymentStatus());
            long expectedCents = expectedUsd == null ? -1 : expectedUsd * 100;
            boolean amountMatches = session.getAmountTotal() != null
                    && session.getAmountTotal() == expectedCents;
            if (!paid || !amountMatches) {
                log.warn("Stripe verification failed: session={} paymentStatus={} amountTotal={} expectedCents={}",
                        sessionId, session.getPaymentStatus(), session.getAmountTotal(), expectedCents);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Stripe verification error for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Verify a PayPal payment is approved and matches the expected USD amount.
     */
    public boolean verifyPaypalApproved(String paypalPaymentId, Long expectedUsd) {
        try {
            APIContext apiContext = new APIContext(paypalClientId, paypalClientSecret, paypalMode);
            Payment payment = Payment.get(apiContext, paypalPaymentId);
            boolean approved = "approved".equals(payment.getState());
            boolean amountMatches = false;
            if (!payment.getTransactions().isEmpty()) {
                BigDecimal total = new BigDecimal(payment.getTransactions().get(0).getAmount().getTotal());
                amountMatches = total.compareTo(BigDecimal.valueOf(expectedUsd)) == 0;
            }
            if (!approved || !amountMatches) {
                log.warn("PayPal verification failed: payment={} state={} expectedUsd={}",
                        paypalPaymentId, payment.getState(), expectedUsd);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("PayPal verification error for payment {}: {}", paypalPaymentId, e.getMessage());
            return false;
        }
    }
}

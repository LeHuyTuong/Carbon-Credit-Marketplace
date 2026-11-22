package com.carbonx.marketcarbon.service.payment.gateway;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Chiến lược tạo Stripe Checkout Session. */
@Component
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    @Value("${payment_success_url}")
    private String successUrl;

    @Value("${payment_cancel_url}")
    private String cancelUrl;

    @Value("${stripe.api.key}")
    private String stripeKey;

    // Optional để vớiDefaults() (unit test / fallback) vẫn tạo được gateway.
    @Autowired(required = false)
    private PaymentOrderRepository paymentOrderRepository;

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentOrderResponse createPayment(User user, Long amount, Long orderId) {
        Stripe.apiKey = stripeKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + orderId)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amount * 100)
                                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                        .builder().setName("Top up wallet").build())
                                                .build())
                                .build())
                .build();

        try {
            Session session = Session.create(params);
            storeProviderRef(orderId, session.getId());
            log.debug("Stripe checkout session created: {}", session.getId());

            PaymentOrderResponse res = new PaymentOrderResponse();
            res.setPayment_url(session.getUrl());
            res.setId(orderId);
            res.setAmount(amount);
            res.setAmountInVnd(com.carbonx.marketcarbon.utils.CurrencyConverter.usdToVnd(
                    BigDecimal.valueOf(amount)));
            return res;
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe checkout session", e);
        }
    }

    private void storeProviderRef(Long orderId, String providerRef) {
        if (paymentOrderRepository == null) {
            return;
        }
        paymentOrderRepository.findById(orderId).ifPresent(po -> {
            po.setProviderRef(providerRef);
            paymentOrderRepository.save(po);
        });
    }
}

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;

    private final UserRepository  userRepository;

    @Value("${stripe.api.key}")
    private String stripeKey;

//    @Value("${paypal.client.id}")
//    private String paypalClientId;
//
//    @Value("${paypal.client.secret}")
//    private String paypalClientSecret;
//
//    @Value("${paypal.mode}")
//    private String paypalMode;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public PaymentOrderResponse createOrder(PaymentOrderRequest request) {
        User user = currentUser();

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .user(user)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(Status.PENDING)
                .build();

        PaymentOrder savedPaymentOrder = paymentOrderRepository.save(paymentOrder);
        return PaymentOrderResponse.builder()
                .userId(user.getId())
                .method(savedPaymentOrder.getPaymentMethod())
                .amount(savedPaymentOrder.getAmount())
                .status(savedPaymentOrder.getStatus())
                .id(savedPaymentOrder.getId())
                .createdDate(savedPaymentOrder.getCreateAt())
                .build();

    }

    @Override
    public List<PaymentOrder> getAllPaymentByUser() {
        User user = currentUser();
        return paymentOrderRepository.findPaymentByUserId(user.getId());
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) {
        return paymentOrderRepository.findPaymentById(id);
    }

    @Override
    public Boolean processPaymentOrder(PaymentOrder paymentOrder, String paymentId) {
        if(paymentOrder.getStatus().equals(Status.PENDING)){

            paymentOrder.setStatus(Status.SUCCEEDED);
            paymentOrderRepository.save(paymentOrder);
            return true;
        }
        return false;
    }

    @Override
    public PaymentOrderResponse createStripePaymentLink(PaymentOrderRequest request, Long orderId) throws StripeException {
        Stripe.apiKey = stripeKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://localhost:5173/wallet?order_id=" + orderId)
                .setCancelUrl("https://localhost:5173/payment/cancal")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(request.getAmount() * 100)
                                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                        .builder().setName("Top up wallet").build())
                                                .build())
                                .build())
                .build();
        Session session = Session.create(params);
        System.out.println("session____ " + session);

        PaymentOrderResponse res = new PaymentOrderResponse();
        res.setPayment_url(session.getUrl());
        return res;
    }

}

package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.PaymentService;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.utils.CurrencyConverter;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SseService sseService;

    @Value("${payment_success_url}")
    private String successUrl;

    @Value("${payment_cancel_url}")
    private String cancelUrl;

    @Value("${stripe.api.key}")
    private String stripeKey;

    @Value("${paypal.client.id}")
    private String paypalClientId;

    @Value("${paypal.client.secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

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
        BigDecimal amountInVnd = CurrencyConverter.usdToVnd(BigDecimal.valueOf(savedPaymentOrder.getAmount()));

        String message = "Create deposit with money "  + request.getAmount() + " USD"  ;
        sseService.sendNotificationToUser(user.getId(), message);

        return PaymentOrderResponse.builder()
                .userId(user.getId())
                .method(savedPaymentOrder.getPaymentMethod())
                .amount(savedPaymentOrder.getAmount())
                .amountInVnd(amountInVnd)
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
    @Transactional
    public Boolean processPaymentOrder(Long orderId, String paymentId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found with id: " + orderId));

        // Nếu đơn đã xử lý rồi thì bỏ qua
        if (order.getStatus() == Status.SUCCEEDED) {
            log.warn("Payment order {} already processed.", order.getId());
            return false;
        }

        // Giả lập xác nhận thanh toán thành công (hoặc gọi API Stripe/VNPay thật ở đây)
        boolean paymentConfirmed = true; // hoặc verifyPaymentFromGateway(paymentId)

        if (paymentConfirmed) {
            // Cập nhật trạng thái đơn
            order.setStatus(Status.SUCCEEDED);
            paymentOrderRepository.save(order);
            // Lấy wallet của user
            Wallet wallet = walletRepository.findByUserId(order.getUser().getId());
            if (wallet == null) {
                throw new ResourceNotFoundException("Wallet not found for user " + order.getUser().getId());
            }
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
                .setSuccessUrl(successUrl + orderId)
                .setCancelUrl(cancelUrl)
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
        res.setId(orderId);
        res.setAmount(request.getAmount());
        res.setAmountInVnd(CurrencyConverter.usdToVnd(BigDecimal.valueOf(request.getAmount())));
        return res;
    }

    @Override
    public PaymentOrderResponse createPayPalPaymentLink(PaymentOrderRequest paymentOrder, Long orderId) throws PayPalRESTException {
        APIContext apiContext = new APIContext(paypalClientId, paypalClientSecret, paypalMode);

        // Create amount details
        Amount paymentAmount = new Amount();
        paymentAmount.setCurrency("USD"); // Change to the appropriate currency
        paymentAmount.setTotal(String.format("%.2f", paymentOrder.getAmount() * 1.0));

        // Create transaction details
        Transaction transaction = new Transaction();
        transaction.setDescription("Payment for Order ID: " + orderId);
        transaction.setAmount(paymentAmount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        // Create payer details
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");


        // Create redirect URLs
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl + orderId);

        // Create payment details
        com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setRedirectUrls(redirectUrls);

        // Create the payment
        com.paypal.api.payments.Payment createdPayment = payment.create(apiContext);

        // Extract approval URL
        String approvalUrl = null;
        for (Links link : createdPayment.getLinks()) {
            if ("approval_url".equals(link.getRel())) {
                approvalUrl = link.getHref();
                break;
            }
        }

        // Create and return payment response
        PaymentOrderResponse res = new PaymentOrderResponse();
        res.setPayment_url(approvalUrl);
        res.setId(orderId);
        res.setAmount(paymentOrder.getAmount());
        res.setAmountInVnd(CurrencyConverter.usdToVnd(BigDecimal.valueOf(paymentOrder.getAmount())));
        return res;
    }

    @Override
    public PaymentOrder createOrderVNPay(Long amount, String orderInfo, String vnp_TxnRef) {
        User user = currentUser();

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .amount(amount)
                .status(Status.PENDING)
                .user(user)
                .vnpTxnRef(vnp_TxnRef)
                .build();
        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public void updateOrderStatus(String vnp_TxnRef, Status status) {
        PaymentOrder order = paymentOrderRepository.findByVnpTxnRef(vnp_TxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Order ID not found with : " + vnp_TxnRef));
        if(order.getStatus().equals(Status.PENDING)){
            order.setStatus(status);
            paymentOrderRepository.save(order);
        }
    }


}

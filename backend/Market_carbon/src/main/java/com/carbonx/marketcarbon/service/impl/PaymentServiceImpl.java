package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.PaymentService;
import com.carbonx.marketcarbon.service.PaymentVerificationService;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final PaymentVerificationService verificationService;
    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;

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

    // =====================================================================
    // P0-B/B1 — server-side deposit confirmation
    //
    // Step 1 (NO transaction): assertDepositVerifiable — ownership check +
    //        provider-side verification. External HTTP must never sit inside a
    //        financial DB transaction (connection held for seconds = pool death).
    // Step 2 (ONE transaction): applyVerifiedDeposit — pessimistic lock on the
    //        payment order, PENDING → SUCCEEDED guard, wallet credit + ledger row.
    //        Concurrent or replayed calls serialize on the lock; the status guard
    //        makes the credit exactly-once.
    // =====================================================================

    @Override
    public void assertDepositVerifiable(Long orderId) {
        User user = currentUser();
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found with id: " + orderId));

        if (!Objects.equals(order.getUser().getId(), user.getId())) {
            // BOLA: without this check any user could confirm someone else's order and
            // have the amount credited to their own wallet.
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (order.getStatus() == Status.SUCCEEDED) {
            return; // idempotent fast path: already credited
        }
        if (order.getStatus() != Status.PENDING) {
            throw new BadRequestException("Payment order " + orderId + " is no longer pending (status=" + order.getStatus() + ")");
        }

        String providerRef = order.getProviderRef();
        if (providerRef == null || providerRef.isBlank()) {
            throw new BadRequestException("Payment order " + orderId + " has no stored provider reference to verify");
        }

        boolean verified;
        switch (order.getPaymentMethod()) {
            case STRIPE -> verified = verificationService.verifyStripePaid(providerRef, order.getAmount());
            case PAYPAL -> verified = verificationService.verifyPaypalApproved(providerRef, order.getAmount());
            default -> {
                // VNPay orders are credited only through the signature-verified return flow
                throw new BadRequestException("Payment method " + order.getPaymentMethod()
                        + " must be confirmed through its provider callback, not this endpoint");
            }
        }
        if (!verified) {
            throw new BadRequestException("Payment could not be verified with the provider for order " + orderId);
        }
    }

    @Override
    @Transactional
    public Wallet applyVerifiedDeposit(Long orderId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found with id: " + orderId));

        if (order.getStatus() != Status.PENDING) {
            // SUCCEEDED (already credited once) or terminal — never credit again
            log.info("Deposit for order {} already processed (status={}), skipping credit", orderId, order.getStatus());
            return walletRepository.findByUserId(order.getUser().getId());
        }

        // Stripe/PayPal orders store whole USD
        creditWalletForOrder(order, BigDecimal.valueOf(order.getAmount()));
        order.setStatus(Status.SUCCEEDED);
        paymentOrderRepository.save(order);
        log.info("Deposit credited for order {} ({} USD) to user {}", orderId, order.getAmount(), order.getUser().getId());
        return walletRepository.findByUserId(order.getUser().getId());
    }

    @Override
    @Transactional
    public boolean applyVnPayDeposit(String vnpTxnRef) {
        PaymentOrder order = paymentOrderRepository.findByVnpTxnRefWithLock(vnpTxnRef)
                .orElse(null);
        if (order == null || order.getStatus() != Status.PENDING) {
            return false; // unknown or already processed — idempotent
        }

        // VNPay orders store VND; wallet balance is USD → convert before crediting.
        BigDecimal amountUsd = BigDecimal.valueOf(order.getAmount())
                .divide(CurrencyConverter.getUsdToVndRate(), 2, RoundingMode.HALF_UP);

        creditWalletForOrder(order, amountUsd);
        order.setStatus(Status.SUCCEEDED);
        paymentOrderRepository.save(order);
        log.info("VNPay deposit credited for txnRef {} ({} VND → {} USD) to user {}",
                vnpTxnRef, order.getAmount(), amountUsd, order.getUser().getId());
        return true;
    }

    /**
     * Credit the order owner's wallet and write the ledger row — caller must hold
     * the payment-order lock and have re-checked PENDING. Runs inside the caller's
     * transaction so status flip + credit + ledger commit or roll back together.
     */
    private void creditWalletForOrder(PaymentOrder order, BigDecimal amountUsd) {
        User owner = order.getUser();
        Wallet wallet = walletRepository.findByUserId(owner.getId());
        if (wallet == null) {
            wallet = walletService.generateWallet(owner);
        }
        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                .wallet(wallet)
                .type(WalletTransactionType.ADD_MONEY)
                .description("Deposit via " + order.getPaymentMethod() + " (order #" + order.getId() + "): "
                        + amountUsd + " USD")
                .amount(amountUsd)
                .build());
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
        // P0-B/B1: remember the provider reference server-side so success can be verified
        // with Stripe later — the frontend only gets the URL and never proves payment.
        paymentOrderRepository.findById(orderId).ifPresent(po -> {
            po.setProviderRef(session.getId());
            paymentOrderRepository.save(po);
        });

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

        // P0-B/B1: store the PayPal payment id server-side for later verification
        paymentOrderRepository.findById(orderId).ifPresent(po -> {
            po.setProviderRef(createdPayment.getId());
            paymentOrderRepository.save(po);
        });

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

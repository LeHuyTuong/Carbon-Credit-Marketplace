package com.carbonx.marketcarbon.service.payment.gateway;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.config.PaypalMode;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentOrderRepository;
import com.carbonx.marketcarbon.utils.CurrencyConverter;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Chiến lược tạo PayPal payment và lấy approval URL. */
@Component
public class PaypalPaymentGateway implements PaymentGateway {

    @Value("${payment_success_url}")
    private String successUrl;

    @Value("${payment_cancel_url}")
    private String cancelUrl;

    @Value("${paypal.client.id}")
    private String paypalClientId;

    @Value("${paypal.client.secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    @Autowired(required = false)
    private PaymentOrderRepository paymentOrderRepository;

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public PaymentOrderResponse createPayment(User user, Long amount, Long orderId) {
        APIContext apiContext = new APIContext(paypalClientId, paypalClientSecret,
                PaypalMode.normalize(paypalMode));

        Amount paymentAmount = new Amount();
        paymentAmount.setCurrency("USD");
        paymentAmount.setTotal(String.format("%.2f", amount * 1.0));

        Transaction transaction = new Transaction();
        transaction.setDescription("Payment for Order ID: " + orderId);
        transaction.setAmount(paymentAmount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl + orderId);

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setRedirectUrls(redirectUrls);

        try {
            Payment createdPayment = payment.create(apiContext);
            storeProviderRef(orderId, createdPayment.getId());

            String approvalUrl = null;
            for (Links link : createdPayment.getLinks()) {
                if ("approval_url".equals(link.getRel())) {
                    approvalUrl = link.getHref();
                    break;
                }
            }

            PaymentOrderResponse res = new PaymentOrderResponse();
            res.setPayment_url(approvalUrl);
            res.setId(orderId);
            res.setAmount(amount);
            res.setAmountInVnd(CurrencyConverter.usdToVnd(BigDecimal.valueOf(amount)));
            return res;
        } catch (PayPalRESTException e) {
            throw new PaymentGatewayException("Failed to create PayPal payment", e);
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

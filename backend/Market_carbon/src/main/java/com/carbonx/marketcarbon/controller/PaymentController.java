package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.PaymentMethod;
import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.PaymentOrderRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.service.PaymentService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/payment")
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<TuongCommonResponse<PaymentOrderResponse>> paymentHandler(
            @Valid @RequestBody TuongCommonRequest<@Valid PaymentOrderRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
    throws StripeException {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PaymentOrderResponse order = paymentService.createOrder(req.getData());
        PaymentOrderResponse response = new PaymentOrderResponse();
        if(req.getData().getPaymentMethod().equals(PaymentMethod.STRIPE)){
            response = paymentService.createStripePaymentLink(req.getData(), order.getId());
        }
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentOrderResponse> resp = new TuongCommonResponse<>(trace, now , rs, response);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<PaymentOrder>>> getAllPaymentsByUser(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws StripeException {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<PaymentOrder> order = paymentService.getAllPaymentByUser();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<PaymentOrder>> resp = new TuongCommonResponse<>(trace, now , rs, order);
        return ResponseEntity.ok(resp);
    }
}

package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.service.*;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;
    private final WalletTransactionService  walletTransactionService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    @Operation(summary = "User wallet" , description = "API get own wallet")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<Wallet>> getUserWallet (
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws WalletException
    {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Wallet wallet = walletService.getUserWallet();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Wallet> response = new TuongCommonResponse<>(trace, now , rs ,wallet );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "deposit money " , description = "API deposit money ")
    @PutMapping("/amount/{amount}")
    public ResponseEntity<TuongCommonResponse<PaymentOrderResponse>> depositMoney(
            @PathVariable("amount") Long amount,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) throws WalletException {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Wallet wallet = walletService.addBalanceToWallet(amount);
        PaymentOrderResponse res = new PaymentOrderResponse();
        res.setPayment_url("deposit success");
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<PaymentOrderResponse> response = new TuongCommonResponse<>(trace,now,rs , res);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<TuongCommonResponse<Wallet>> addMoneyToWallet(
            @RequestParam(name = "order_id") Long orderId,
            @RequestParam(name = "payment_id") String paymentId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws WalletException{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentOrder order = paymentService.getPaymentOrderById(orderId);
        Boolean status = paymentService.processPaymentOrder(order, paymentId);

        PaymentOrderResponse res = new PaymentOrderResponse();
        res.setPayment_url("deposit success");
        Wallet wallet =  walletService.getUserWallet();
        if(status){
            wallet = walletService.addBalanceToWallet(order.getAmount());
        }
        TuongResponseStatus rs =  new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Wallet> response = new TuongCommonResponse<>(trace,now,rs,wallet);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get history of transactions", description = "API Get history of transaction")
    @GetMapping("/transactions")
    public ResponseEntity<TuongCommonResponse<List<WalletTransaction>>> getTransactions (
            @Valid @RequestBody TuongCommonRequest<WalletTransactionRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<WalletTransaction> transactions = walletTransactionService.getTransaction(req.getData().getWallet(), null);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<WalletTransaction>> response = new TuongCommonResponse<>(trace, now , rs ,transactions );
        return ResponseEntity.ok(response);
    }
// TODO
//    @PutMapping("/order/{orderId}/pay")
//    public ResponseEntity<TuongCommonResponse<Wallet>> payOrder(
//            @PathVariable("orderId") Long orderId,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//    )throws Exception{
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        Order order = orderService.getOrderById(orderId);
//        Wallet wallet = walletService.;
//    }



}

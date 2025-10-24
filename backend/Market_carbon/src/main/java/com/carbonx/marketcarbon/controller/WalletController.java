package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.PaymentOrderResponse;
import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.PaymentOrder;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.service.*;
import com.carbonx.marketcarbon.utils.CurrencyConverter;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final WalletTransactionService  walletTransactionService;
    private final PaymentService paymentService;

    @Operation(summary = "User wallet" , description = "API get own wallet")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<WalletResponse>> getUserWallet (
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws WalletException
    {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        WalletResponse wallet = walletService.getUserWallet();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<WalletResponse> response = new TuongCommonResponse<>(trace, now , rs ,wallet );
        return ResponseEntity.ok(response);
    }

//    @Operation(summary = "API add money to Wallet   " , description = "API deposit money ")
//    @PutMapping("/deposit/amount/{amount}")
//    public ResponseEntity<TuongCommonResponse<PaymentOrderResponse>> depositMoney(
//            @PathVariable("amount") Long amount,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//    ) throws WalletException {
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        BigDecimal amountInVnd = CurrencyConverter.usdToVnd(BigDecimal.valueOf(amount));
//        PaymentOrderResponse res = new PaymentOrderResponse();
//        res.setPayment_url("deposit success");
//        res.setAmount(amount);
//        res.setAmountInVnd(amountInVnd);
//        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
//                StatusCode.SUCCESS.getMessage());
//        TuongCommonResponse<PaymentOrderResponse> response = new TuongCommonResponse<>(trace,now,rs , res);
//        return ResponseEntity.ok(response);
//    }

    @Operation(summary = "Set status pending to success ", description = "API change status to confirm money in wallet ")
    @PostMapping("/deposit")
    public ResponseEntity<TuongCommonResponse<WalletResponse>> addMoneyToWallet(
            @RequestParam(name = "order_id") Long orderId,
            @RequestParam(name = "payment_id") String paymentId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws WalletException{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        PaymentOrder order = paymentService.getPaymentOrderById(orderId);

        Boolean status = paymentService.processPaymentOrder(order, paymentId);

        WalletResponse walletDto = null;
        if (status) {
            // If payment succeeded, add balance to the wallet and get the updated wallet DTO
            walletDto = walletService.addBalanceToWallet(order.getAmount()); // addBalanceToWallet now returns DTO
        } else {
            // If payment failed or was already processed, just get the current wallet state
            log.warn("Payment order {} status was not updated or already processed. Fetching current wallet state.", orderId);
            walletDto = walletService.getUserWallet(); // Get current wallet DTO without adding balance
        }

        TuongResponseStatus rs =  new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<WalletResponse> response = new TuongCommonResponse<>(trace,now,rs,walletDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get history of transactions", description = "API Get history of transaction")
    @GetMapping("/transactions")
    public ResponseEntity<TuongCommonResponse<List<WalletTransactionResponse>>> getTransactions (
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactions(); // Call service method returning DTO list

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<WalletTransactionResponse>> response = new TuongCommonResponse<>(trace, now , rs ,transactionDtos );
        return ResponseEntity.ok(response);
    }
/*

    @PutMapping("/order/{orderId}/pay")
    public ResponseEntity<TuongCommonResponse<Wallet>> payOrder(
            @PathVariable("orderId") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    )throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Order order = orderService.getOrderById(orderId);

    }

*/

}

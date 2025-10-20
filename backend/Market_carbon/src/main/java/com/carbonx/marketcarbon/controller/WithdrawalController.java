package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.WithdrawalService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/withdrawal")
public class WithdrawalController {

    private final WithdrawalService  withdrawalService;

    private final WalletService walletService;

    private final WalletTransactionService walletTransactionService;

    @Operation(summary = "Request withdrawal money ", description = "Api user request withdrawal money ")
    @PostMapping("/{amount}")
    public ResponseEntity<TuongCommonResponse<Withdrawal>> withdrawalRequest(
            @PathVariable("amount") Long amount,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Wallet userWallet = walletService.getUserWallet();

        Withdrawal withdrawal = withdrawalService.requestWithdrawal(amount);
        walletService.addBalanceToWallet( -amount);

        WalletTransactionRequest walletTransactionRequest =  WalletTransactionRequest.builder()
                .wallet(userWallet)
                .type(WalletTransactionType.WITH_DRAWL)
                .description("Bank account withdrawal")
                .amount(withdrawal.getAmount().negate())
                .build();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Withdrawal> response = new TuongCommonResponse<>(trace, now , rs, withdrawal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Admin accept withdrawal" , description = "API process withdrawal , admin accept")
    @PatchMapping("/admin/{id}/process/{accept}")
    public ResponseEntity<TuongCommonResponse<Withdrawal>> processWithdrawal(
            @PathVariable("id") Long id,
            @PathVariable("accept") boolean accept,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    )throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Withdrawal withdrawal = withdrawalService.processWithdrawal(id, accept);
        if(!accept){
            walletService.addBalanceToWallet(withdrawal.getAmount().longValue());
        }

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Withdrawal> response = new TuongCommonResponse<>(trace,now,rs,withdrawal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get withdrawl history" , description = "API to get all history of User withdrawl request")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<Withdrawal>>> getWithdrawalHistory(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<Withdrawal> withdrawals = withdrawalService.getUsersWithdrawalHistory();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<Withdrawal>> response = new TuongCommonResponse<>(trace,now,rs,withdrawals);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get withdrawl history by admin" , description = "API admin to get all history of User withdrawl request")
    @GetMapping("/admin")
    public ResponseEntity<TuongCommonResponse<List<Withdrawal>>> getALlWithdrawalRequest(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        List<Withdrawal> withdrawals = withdrawalService.getAllWithdrawalRequest();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<Withdrawal>> response = new TuongCommonResponse<>(trace,now,rs,withdrawals);
        return ResponseEntity.ok(response);
    }



}

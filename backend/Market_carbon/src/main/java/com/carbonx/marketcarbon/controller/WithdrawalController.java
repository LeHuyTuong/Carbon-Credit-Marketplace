package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
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
    public ResponseEntity<TuongCommonResponse<WalletTransactionResponse>> withdrawalRequest(
            @PathVariable("amount") Long amount,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws Exception{
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        // 1. Request withdrawal creation (creates the Withdrawal entity)
        // Assuming 'amount' is the POSITIVE amount the user wants to withdraw
        Withdrawal withdrawal = withdrawalService.requestWithdrawal(amount);

        // 2. Get the Wallet *ENTITY* needed for the transaction
        // First get the WalletResponse to know the ID
        WalletResponse userWalletResponse = walletService.getUserWallet();
        // Then fetch the actual Wallet entity using the ID
        Wallet userWalletEntity = walletService.findWalletEntityById(userWalletResponse.getId());

        // 3. Create the withdrawal transaction request DTO using the Wallet entity
        WalletTransactionRequest transactionRequest = WalletTransactionRequest.builder()
                .wallet(userWalletEntity) // Use the fetched Wallet entity
                .type(WalletTransactionType.WITHDRAWAL)
                .description("Bank account withdrawal request initiated. ID: " + withdrawal.getId())
                // Pass the POSITIVE amount requested by the user
                .amount(withdrawal.getAmount())
                .build();

        // 4. Create the transaction (this service call now returns a DTO)
        WalletTransactionResponse createdTransaction = walletTransactionService.createTransaction(transactionRequest);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<WalletTransactionResponse> response = new TuongCommonResponse<>(trace, now , rs, createdTransaction);
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

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Withdrawal> response = new TuongCommonResponse<>(trace,now,rs,withdrawal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get withdrawal history" , description = "API to get all history of User withdrawal request")
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

    @Operation(summary = "Get withdrawal history by admin" , description = "API admin to get all history of User withdrawal request")
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

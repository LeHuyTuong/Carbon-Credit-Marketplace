package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
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

    @Operation(summary = "User wallet" , description = "API get own wallet")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<Wallet>> getWallet (
            @RequestHeader("Authorization") String bearerToken,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
            throws WalletException
    {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        String jwt = bearerToken.substring(7);
        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Wallet> response = new TuongCommonResponse<>(trace, now , rs ,wallet );
        return ResponseEntity.ok(response);
    }
//    TODO : đợi upload method thanh toán là paypal rồi làm tiếp
//    @PutMapping("/api/wallet/deposit")
//    public ResponseEntity<TuongCommonResponse<Wallet>> addMoneyToWallet(
//            @RequestHeader("Authorization") String bearerToken,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime)
//            throws WalletException{
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        String jwt = bearerToken.substring(7);
//        User user = userService.findUserProfileByJwt(jwt);
//        Wallet wallet = walletService.getUserWallet(user);
//
//
//    }

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

}

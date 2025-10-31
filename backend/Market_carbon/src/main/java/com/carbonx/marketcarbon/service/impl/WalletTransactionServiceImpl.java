package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

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
    @Transactional
    public WalletTransactionResponse createTransaction(WalletTransactionRequest request) {

        // check wallet
        Wallet wallet;
        if (request.getWallet() != null && request.getWallet().getId() != null) {
            wallet = walletRepository.findById(request.getWallet().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found for transaction with ID: " + request.getWallet().getId()));
        } else {
            throw new IllegalArgumentException("Wallet ID must be provided in WalletTransactionRequest");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Transaction type cannot be null in WalletTransactionRequest");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null in WalletTransactionRequest");
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount must be different from zero");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter;

        if (request.getType() == WalletTransactionType.WITHDRAWAL ||
            request.getType() == WalletTransactionType.BUY_CARBON_CREDIT
        )
        {
            // For subtraction, the requested amount should likely be positive, then negated logic applies
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount for withdrawal should be positive.");
            }
            balanceAfter = balanceBefore.subtract(amount);
        }
        else if (request.getType() == WalletTransactionType.ADD_MONEY ||
        request.getType() == WalletTransactionType.SELL_CARBON_CREDIT)
        {
            // phải dương
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount for deposit/receive types should be positive.");
            }
            balanceAfter = balanceBefore.add(amount);
        } else {
            // nếu không thì không làm gì cả sau trước như 1
            balanceAfter = balanceBefore;
        }

        // so tien sau khi nap ko dc am
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient balance for transaction");
        }

        // update và lưu lại balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // tạo and lưu the transaction entity
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setOrder(request.getOrder());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

        // Map the saved entity to DTO and return
        return mapToTransactionResponse(savedTransaction);
    }

    @Override
    @Transactional
    public List<WalletTransactionResponse> getTransactions() {
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            // Nếu user chưa có ví, trả về danh sách rỗng
            return List.of();
        }
        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
        // Map entities to DTOs
        return transactions.stream()
                .map(this::mapToTransactionResponse) // sử dụng stream map
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<WalletTransactionResponse> getTransactionDtosForWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
        // Map entities to DTOs
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countMyTransactions() {
        User user = currentUser();
        return walletTransactionRepository.countByWalletUserId(user.getId());
    }

    @Override
    public long countAllTransactions() {
        return walletTransactionRepository.count();
    }

    // Helper method to map WalletTransaction entity to WalletTransactionResponse DTO
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        Order order = transaction.getOrder();
        LocalDate creditExpiryDate = null;

        CarbonCredit orderCredit = (order != null) ? order.getCarbonCredit() : null;
        if (orderCredit != null) {
            creditExpiryDate = orderCredit.getExpiryDate();
        }

        if (creditExpiryDate == null && transaction.getCreditBatch() != null) {
            creditExpiryDate = transaction.getCreditBatch().getExpiresAt();
        }
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .orderId(order != null ? order.getId() : null)
                .transactionType(transaction.getTransactionType())
                .description(transaction.getDescription())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .amount(transaction.getAmount())
                // Lưu thêm số lượng tín chỉ và đơn giá hiện thị cho client
                .carbonCreditQuantity(order != null ? order.getQuantity() : null)
                .unitPrice(order != null ? order.getUnitPrice() : null)
                .createdAt(transaction.getCreatedAt())
                .batchCode(transaction.getCreditBatch() != null
                        ? transaction.getCreditBatch().getBatchCode()
                        : null)
                .creditExpiryDate(creditExpiryDate)
                .build();
    }
}

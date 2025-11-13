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
import java.util.*;
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
        // 1. Lấy CarbonCredit từ wallet
        CarbonCredit carbonCredit = wallet.getCarbonCredit();
        CreditBatch creditBatch = null; // 2. Mặc định batch là null
        // 3. Chỉ lấy batch nếu carbonCredit không null
        if (carbonCredit != null) {
            creditBatch = carbonCredit.getBatch();
        }
        // 4. Gán batch (có thể là null nếu không tìm thấy)
        transaction.setCreditBatch(creditBatch);

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

        // Gọi query đã được tối ưu (từ Bước 1)
        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);

        // === LOGIC GỘP GIAO DỊCH (MỚI) ===

        // 1. Phân loại giao dịch: Gộp các khoản chi PROFIT_SHARING, giữ nguyên các giao dịch khác
        Map<Long, List<WalletTransaction>> groupedProfitSharing = new LinkedHashMap<>();
        List<WalletTransaction> otherTransactions = new ArrayList<>();

        for (WalletTransaction tx : transactions) {
            // Chỉ gộp các giao dịch CHI TIỀN (amount < 0) và có cùng distributionId
            if (tx.getTransactionType() == WalletTransactionType.PROFIT_SHARING &&
                    tx.getDistribution() != null &&
                    tx.getAmount() != null &&
                    tx.getAmount().compareTo(BigDecimal.ZERO) < 0) {

                groupedProfitSharing.computeIfAbsent(
                        tx.getDistribution().getId(),
                        k -> new ArrayList<>()
                ).add(tx);
            } else {
                otherTransactions.add(tx);
            }
        }

        // 2. Xử lý các nhóm đã gộp
        List<WalletTransaction> mergedTransactions = new ArrayList<>();
        for (List<WalletTransaction> group : groupedProfitSharing.values()) {
            if (group.isEmpty()) continue;

            if (group.size() == 1) {
                // Nếu nhóm chỉ có 1, giữ nguyên
                mergedTransactions.add(group.get(0));
            } else {
                // Nếu nhóm có > 1, tạo giao dịch ảo (virtual transaction)
                WalletTransaction newestTx = group.get(0); // Giao dịch mới nhất (vì list đã sắp xếp DESC)
                WalletTransaction oldestTx = group.get(group.size() - 1); // Giao dịch cũ nhất

                // Tính tổng số tiền
                BigDecimal totalAmount = group.stream()
                        .map(WalletTransaction::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Lấy số dư ĐẦU của giao dịch CŨ NHẤT
                BigDecimal balanceBefore = oldestTx.getBalanceBefore();
                // Lấy số dư SAU của giao dịch MỚI NHẤT
                BigDecimal balanceAfter = newestTx.getBalanceAfter();

                // Tạo một thực thể WalletTransaction "ảo" để gộp
                WalletTransaction mergedTx = new WalletTransaction();
                mergedTx.setId(newestTx.getId()); // Dùng ID của giao dịch mới nhất làm đại diện
                mergedTx.setWallet(newestTx.getWallet());
                mergedTx.setTransactionType(WalletTransactionType.PROFIT_SHARING);
                mergedTx.setDescription(String.format("Payout for distribution #%d (%d owners)",
                        newestTx.getDistribution().getId(), group.size()));
                mergedTx.setBalanceBefore(balanceBefore);
                mergedTx.setBalanceAfter(balanceAfter);
                mergedTx.setAmount(totalAmount); // Tổng số tiền (âm)
                mergedTx.setCreatedAt(newestTx.getCreatedAt()); // Dùng thời gian mới nhất
                mergedTx.setDistribution(newestTx.getDistribution());

                mergedTransactions.add(mergedTx);
            }
        }

        // 3. Thêm lại các giao dịch khác
        mergedTransactions.addAll(otherTransactions);

        // 4. Sắp xếp lại list tổng
        mergedTransactions.sort(Comparator.comparing(WalletTransaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        // Map entities (đã gộp) to DTOs
        return mergedTransactions.stream()
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
                .distributionId(transaction.getDistribution() != null
                        ? transaction.getDistribution().getId()
                        : null)
                .creditExpiryDate(creditExpiryDate)
                .build();
    }
}

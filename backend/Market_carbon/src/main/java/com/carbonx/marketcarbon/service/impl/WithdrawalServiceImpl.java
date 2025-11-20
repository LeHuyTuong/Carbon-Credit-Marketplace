package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.WithdrawalService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final ApplicationNotificationService applicationNotificationService;
    private final SseService sseService;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Transactional
    @Override
    public Withdrawal requestWithdrawal(Long amount) {
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (amount < 2) {
            throw new AppException(ErrorCode.WITHDRAWAL_MONEY_INVALID_AMOUNT);
        }
        // B1 tạo request withdrawal

        BigDecimal withdrawalAmount = BigDecimal.valueOf(amount);

        if (wallet.getBalance().compareTo(withdrawalAmount) >= 0) {
            Withdrawal withdrawal = Withdrawal.builder()
                    .amount(withdrawalAmount)
                    .status(Status.PENDING)
                    .requestedAt(LocalDateTime.now(VIETNAM_ZONE))
                    .user(user)
                    .build();

            log.info("Withdrawal with id {} has been sent with money ", withdrawal.getId());

            String message = " deposit with money " + withdrawalAmount + " USD";
            sseService.sendNotificationToUser(user.getId(), message);

            return withdrawalRepository.save(withdrawal);
        } else {
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }
    }

    @Override
    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) throws Exception {
        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);

        if (withdrawal.isEmpty()) {
            throw new ResourceNotFoundException("Withdrawal not found with id: " + withdrawalId);
        }

        Withdrawal withdrawalRequest = withdrawal.get();
        withdrawalRequest.setProcessedAt(LocalDateTime.now(VIETNAM_ZONE));
        String reason = ""; // reason
        User user = withdrawalRequest.getUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        BigDecimal amountToWithdraw = withdrawalRequest.getAmount();

        if (accept) {
            if (wallet.getBalance().compareTo(amountToWithdraw) < 0) {
                withdrawalRequest.setStatus(Status.FAILED);
                withdrawalRepository.save(withdrawalRequest);
                throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
            }

            withdrawalRequest.setStatus(Status.SUCCEEDED);
            Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawalRequest);
            // send notification
            try {
                applicationNotificationService.sendAdminConfirmRequestWithdrawal(
                        user.getEmail(),
                        user.getEmail(),
                        withdrawalRequest.getId(),
                        amountToWithdraw,
                        withdrawalRequest.getRequestedAt());
            } catch (Exception e) {
                log.warn("Failed to send withdrawal confirmation email via notification service for user {}: {}",
                        user.getEmail(), e.getMessage());
            }

            log.info("Withdrawal with id {} has been sent", withdrawalRequest.getId());
            String message = " deposit with money " + amountToWithdraw + " USD";
            sseService.sendNotificationToUser(user.getId(), message);

            return savedWithdrawal;
        } else {
            withdrawalRequest.setStatus(Status.REJECTED);
            reason = "Withdrawal request rejected by administrator."; // Lý do bị từ chối
            wallet.setBalance(wallet.getBalance().add(amountToWithdraw));
            walletRepository.save(wallet);
        }
        // Lưu trạng thái FAILED hoặc REJECTED
        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawalRequest);

        // Gửi email thất bại/từ chối (chỉ khi FAILED hoặc REJECTED)
        if (savedWithdrawal.getStatus() == Status.FAILED || savedWithdrawal.getStatus() == Status.REJECTED) {
            try {
                log.info("Withdrawal with id {} has been sent", withdrawalRequest.getId());
                applicationNotificationService.sendWithdrawalFailedOrRejected(
                        user.getEmail(),
                        user.getEmail(),
                        savedWithdrawal.getId(),
                        amountToWithdraw,
                        reason, // Truyền lý do vào hàm gửi mail
                        savedWithdrawal.getProcessedAt() // Sử dụng thời gian đã xử lý
                );
            } catch (Exception e) {
                log.warn("Failed to send withdrawal failed/rejected email for user {}: {}", user.getEmail(),
                        e.getMessage());
            }
        }
        // Ném Exception sau khi lưu và gửi mail FAILED để báo lỗi rõ ràng
        if (savedWithdrawal.getStatus() == Status.FAILED) {
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }
        return savedWithdrawal; // Trả về withdrawal với trạng thái FAILED/REJECTED
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory() {
        User user = currentUser();
        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAll();
    }
}

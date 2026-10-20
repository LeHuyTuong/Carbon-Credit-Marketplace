package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.helper.notification.ApplicationNotificationService;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.PaymentDetailsRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.SseService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final PaymentDetailsRepository paymentDetailsRepository;
    private final ApplicationNotificationService applicationNotificationService;
    private final SseService sseService;
    private final WalletTransactionService walletTransactionService;

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
    public Withdrawal requestWithdrawal(BigDecimal amount) {
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            // Người dùng chưa từng mở ví nên chưa có row trong `wallets` (ví được tạo
            // lazily khi mở trang ví). Báo rõ thay vì dereference null -> 500.
            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.WITHDRAWAL_MONEY_INVALID_AMOUNT);
        }
        // B1 tạo request withdrawal

        BigDecimal withdrawalAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP);

        if (wallet.getBalance().compareTo(withdrawalAmount) >= 0) {
            // Lưu khoản người dùng nhận tiền vào chính row withdrawal: nếu không, admin
            // chỉ thấy "N/A" và không biết chuyển tiền đi đâu — tiền bị kẹt vô thời hạn.
            // Chỉ dùng tài khoản NGƯỜI DÙNG (chưa bị gỡ): tài khoản đã gỡ thì người dùng
            // cho là đã xoá, không được chuyển tiền vào đó.
            PaymentDetails paymentDetails = paymentDetailsRepository
                    .findFirstByUserIdAndArchivedFalseOrderByIdDesc(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_DETAILS_NOT_FOUND));
            Withdrawal withdrawal = Withdrawal.builder()
                    .amount(withdrawalAmount)
                    .status(Status.PENDING)
                    .requestedAt(LocalDateTime.now(VIETNAM_ZONE))
                    .user(user)
                    .paymentDetails(paymentDetails)
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
        // P0-A (N3): lock the row so concurrent process attempts serialize,
        // then guard the state transition — only a PENDING withdrawal may be processed.
        Withdrawal withdrawalRequest = withdrawalRepository.findByIdWithPessimisticLock(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found with id: " + withdrawalId));

        if (withdrawalRequest.getStatus() != Status.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Nạp proxy PaymentDetails TRONG transaction: nếu không, entity trả ra ngoài
        // transaction sẽ mang proxy của Hibernate và Jackson không serialize được ->
        // endpoint trả 500 SAU khi tiền đã được chuyển/hoàn.
        if (withdrawalRequest.getPaymentDetails() != null) {
            withdrawalRequest.getPaymentDetails().getAccountHolderName();
        }

        withdrawalRequest.setProcessedAt(LocalDateTime.now(VIETNAM_ZONE));
        String reason = ""; // reason
        User user = withdrawalRequest.getUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            // Không có ví thì không hoàn được tiền: báo rõ thay vì NPE.
            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
        }
        BigDecimal amountToWithdraw = withdrawalRequest.getAmount();

        if (accept) {
//            if (wallet.getBalance().compareTo(amountToWithdraw) < 0) {
//                withdrawalRequest.setStatus(Status.FAILED);
//                withdrawalRepository.save(withdrawalRequest);
//                throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
//            }

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
            // P0-A (N3): refund through the audited transaction path (ledger row + balance
            // update in one mechanism) instead of a bare setBalance. The PENDING guard above
            // makes this refund happen exactly once even under concurrent/replayed calls.
            // Người dùng phải nhận được thông báo realtime cho CẢ hai kết quả (duyệt và
            // từ chối) — nhánh duyệt đã bắn SSE, nhánh từ chối thì chưa.
            sseService.sendNotificationToUser(user.getId(),
                    "Withdrawal #" + withdrawalRequest.getId() + " was rejected — amount returned to wallet");

            walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                    .wallet(wallet)
                    .withdrawal(withdrawalRequest)
                    .type(WalletTransactionType.WITHDRAWAL_REFUND)
                    .description("Withdrawal #" + withdrawalRequest.getId()
                            + " rejected — amount returned to wallet")
                    .amount(amountToWithdraw)
                    .build());
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
        return withdrawalRepository.findByUserIdWithPaymentDetails(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAllWithPaymentDetails();
    }
}

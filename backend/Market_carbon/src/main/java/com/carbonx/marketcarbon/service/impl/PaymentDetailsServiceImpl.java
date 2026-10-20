package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentDetailsRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.PaymentDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentDetailsServiceImpl implements PaymentDetailsService {

    private final PaymentDetailsRepository paymentDetailsRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;

    @Override
    public PaymentDetails addPaymentDetails(PaymentDetailsRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        PaymentDetails paymentDetails = PaymentDetails.builder()
                .user(user)
                .accountNumber(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .accountHolderName(request.getAccountHolderName())
                .customerName(request.getCustomerName())
                .build();
        return  paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    public PaymentDetails updatePaymentDetails(PaymentDetailsRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        PaymentDetails paymentDetails = PaymentDetails.builder()
                .user(user)
                .build();

        paymentDetails.setAccountHolderName(request.getAccountHolderName());
        paymentDetails.setCustomerName(request.getCustomerName());
        paymentDetails.setBankCode(request.getBankCode());
        paymentDetails.setAccountNumber(request.getAccountNumber());

        paymentDetailsRepository.save(paymentDetails);

        return  PaymentDetails.builder()
                .user(user)
                .accountNumber(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .accountHolderName(request.getAccountHolderName())
                .customerName(request.getCustomerName())
                .build();

    }

    @Override
    public void deletePaymentDetails(PaymentDetailsRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        PaymentDetails paymentDetails = paymentDetailsRepository
                .findByUserIdAndAccountNumber(user.getId(), request.getAccountNumber())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_DETAILS_NOT_FOUND));

        // KHÔNG xoá hẳn row: các đơn rút tiền đã xử lý trỏ tới tài khoản này, xoá đi
        // là mất thông tin nơi nhận tiền (và FK cũng chặn xoá). Gỡ = đánh dấu archived;
        // đơn còn PENDING thì phải giữ tài khoản để admin còn biết chuyển tiền đi đâu.
        List<Withdrawal> withdrawals = withdrawalRepository.findByPaymentDetailsId(paymentDetails.getId());
        boolean hasPending = withdrawals.stream()
                .anyMatch(w -> w.getStatus() == com.carbonx.marketcarbon.common.Status.PENDING);
        if (hasPending) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_IN_USE);
        }

        paymentDetails.setArchived(true);
        paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    public PaymentDetails getUserPaymentDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        return paymentDetailsRepository.findFirstByUserIdAndArchivedFalseOrderByIdDesc(user.getId())
                .orElse(null);
    }
}

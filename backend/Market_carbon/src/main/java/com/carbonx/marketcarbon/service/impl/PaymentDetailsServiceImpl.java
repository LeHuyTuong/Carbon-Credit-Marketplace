package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentDetailsRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.PaymentDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentDetailsServiceImpl implements PaymentDetailsService {

    private final PaymentDetailsRepository paymentDetailsRepository;
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

        paymentDetailsRepository.delete(paymentDetails);
    }

    @Override
    public PaymentDetails getUserPaymentDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        return paymentDetailsRepository.findFirstByUserIdOrderByIdDesc(user.getId()).orElse(null);
    }
}

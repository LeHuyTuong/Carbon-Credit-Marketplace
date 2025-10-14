package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.model.PaymentDetails;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.PaymentDetailsRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentDetailsService implements com.carbonx.marketcarbon.service.PaymentDetailsService {

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
    public PaymentDetails getUserPaymentDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        return paymentDetailsRepository.getPaymentDetailsByUserId(user.getId());
    }
}

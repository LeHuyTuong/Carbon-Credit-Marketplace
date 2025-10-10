package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawalRepository withdrawalRepository;

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
    public Withdrawal requestWithdrawal(Long amount) {
        User user = currentUser();
        //B1 tạo request withdrawal
        Withdrawal withdrawal = Withdrawal.builder()
                .amount(amount)
                .status(Status.PENDING)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) throws Exception {
        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);

        if(withdrawal.isEmpty()){
            throw new ResourceNotFoundException("Withdrawal not found with id: " + withdrawalId);
        }

        Withdrawal withdrawalRequest = withdrawal.get();
        withdrawalRequest.setCreatedAt(LocalDateTime.now());
        if(accept){
            withdrawalRequest.setStatus(Status.SUCCEEDED);
        }else{
            withdrawalRequest.setStatus(Status.REJECTED);
        }

        return withdrawalRepository.save(withdrawalRequest);
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

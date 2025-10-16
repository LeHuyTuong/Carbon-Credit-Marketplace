package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.Withdrawal;

import java.util.List;

public interface WithdrawalService {
    Withdrawal requestWithdrawal(Long amount);

    Withdrawal processWithdrawal(Long  withdrawalId, boolean accept) throws Exception;

    List<Withdrawal> getUsersWithdrawalHistory();

    List<Withdrawal> getAllWithdrawalRequest();
}

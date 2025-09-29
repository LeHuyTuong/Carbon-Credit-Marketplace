package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.PasswordResetToken;

public interface PasswordResetTokenService {

    public PasswordResetToken findByToken(String token);

    public void delete(PasswordResetToken resetToken);

}


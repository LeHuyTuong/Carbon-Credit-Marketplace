package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ChangePasswordRequest;
import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.PasswordCreationRequest;
import com.carbonx.marketcarbon.dto.request.UserCreationRequest;
import com.carbonx.marketcarbon.dto.response.UserResponse;
import com.carbonx.marketcarbon.model.User;
import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface UserService {
     User findUserProfileByJwt(String jwt);
     User findUserByEmail(String email);
     List<User> findALlUser();
    void updatePassword(User user, String newPassword);
    void sendPasswordResetEmail(User user);
    void sendOtpForgotPassword(EmailRequest request)
            throws MessagingException, UnsupportedEncodingException;
     UserResponse createUser(UserCreationRequest request, String otp);
     void resetPassword(PasswordCreationRequest request);
     void changePassword(String jwt, ChangePasswordRequest req);
}

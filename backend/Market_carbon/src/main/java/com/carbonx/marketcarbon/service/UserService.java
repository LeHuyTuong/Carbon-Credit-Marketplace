package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
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
    void sendOtpForgotPassword(EmailRequest request)
            throws MessagingException, UnsupportedEncodingException;
     UserResponse createUser(UserCreationRequest request, String otp);
     MessageResponse resetPassword(ResetPasswordRequest req, String bearerToken);
     void changePassword(String jwt, ChangePasswordRequest req);
    User findUserById(Long id);

    void resendOtpForgotPassword(EmailRequest request)
            throws MessagingException, UnsupportedEncodingException;

    long countUsers();

}

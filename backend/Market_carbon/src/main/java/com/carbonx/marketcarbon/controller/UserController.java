package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.PasswordCreationRequest;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1")
public class UserController {
    UserService userService;

    @PostMapping("/check-exists-user")
    public CommonResponse<Boolean> checkExistsUser(@RequestBody EmailRequest request) {
        User user = userService.findUserByEmail(request.getEmail());
        boolean exists = (user != null);

        return CommonResponse.<Boolean>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(
                        new CommonResponse.ResponseStatus(
                                String.valueOf(HttpStatus.OK.value()),
                                "Check exists user success"
                        )
                )
                .responseData(exists)
                .build();
    }

    @PostMapping("/send-otp")
    CommonResponse<Void> sendOtpForgotPassword(@RequestBody EmailRequest request)
            throws MessagingException, UnsupportedEncodingException {

        userService.sendOtpForgotPassword(request);

        return CommonResponse.<Void>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(
                        new CommonResponse.ResponseStatus(
                                String.valueOf(HttpStatus.OK.value()),
                                "Send OTP Successfully"
                        )
                )
                .build();
    }

    @PostMapping("/reset-password")
    CommonResponse<Void> resetPassword(@RequestBody @Valid PasswordCreationRequest request ){
        userService.resetPassword(request);

        return CommonResponse.<Void>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(
                        new CommonResponse.ResponseStatus(
                                String.valueOf(HttpStatus.OK.value()),
                                "Reset Password Successfully"
                        )
                )
                .build();
    }
}
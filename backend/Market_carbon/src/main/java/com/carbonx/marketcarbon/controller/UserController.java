package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.request.ChangePasswordRequest;
import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.PasswordCreationRequest;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

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
    @PostMapping("/change-password")
    public ResponseEntity<CommonResponse<MessageResponse>> changePassword(
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody ChangePasswordRequest req) {

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String jwt = bearerToken.substring(7); // cắt "Bearer "

        userService.changePassword(jwt, req);

        return ResponseEntity.ok(
                ResponseUtil.success("trace-change-password",
                        new MessageResponse("Password changed successfully"))
        );
    }
}

package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.dto.request.ChangePasswordRequest;
import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.PasswordCreationRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    @GetMapping("/me/profile")
    public CommonResponse<User> getMyProfile(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String jwt = bearerToken.substring(7);
        User me = userService.findUserProfileByJwt(jwt);

        return CommonResponse.<User>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(new CommonResponse.ResponseStatus(
                        String.valueOf(HttpStatus.OK.value()),
                        "Get profile successfully"
                ))
                .responseData(me)
                .build();
    }

    // TÌM USER THEO EMAIL (phục vụ kiểm tra, hiển thị chi tiết) — có thể hạn chế cho ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/by-email")
    public CommonResponse<User> getUserByEmail(@RequestParam("email") String email) {
        User user = userService.findUserByEmail(email);
        return CommonResponse.<User>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(new CommonResponse.ResponseStatus(
                        String.valueOf(HttpStatus.OK.value()),
                        "Get user by email successfully"
                ))
                .responseData(user)
                .build();
    }

    // LẤY TOÀN BỘ USER (đơn giản, chưa phân trang) — nên chỉ mở cho ADMIN
     @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public CommonResponse<java.util.List<User>> getAllUsers() {
        java.util.List<User> users = userService.findALlUser();
        return CommonResponse.<java.util.List<User>>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(new CommonResponse.ResponseStatus(
                        String.valueOf(HttpStatus.OK.value()),
                        "Get all users successfully"
                ))
                .responseData(users)
                .build();
    }

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

    @PostMapping("/send-otp-forgot")
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
    public CommonResponse<MessageResponse> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request,
            @RequestHeader("Authorization") String bearerToken) {

        MessageResponse result = userService.resetPassword(request, bearerToken);

        return CommonResponse.<MessageResponse>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(new CommonResponse.ResponseStatus(
                        String.valueOf(HttpStatus.OK.value()),
                        result.getMessage()
                ))
                .responseData(result)
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<CommonResponse<MessageResponse>> changePassword(
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody ChangePasswordRequest req) {

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String jwt = bearerToken.substring(7);

        userService.changePassword(jwt, req);

        return ResponseEntity.ok(
                ResponseUtil.success("trace-change-password",
                        new MessageResponse("Password changed successfully"))
        );
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{id}")
    public CommonResponse<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return CommonResponse.<User>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(new CommonResponse.ResponseStatus(
                        String.valueOf(HttpStatus.OK.value()),
                        "Get user by ID successfully"
                ))
                .responseData(user)
                .build();
    }

    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<CommonResponse<MessageResponse>> resendOtp(@RequestBody EmailRequest req)
            throws Exception {

        userService.resendOtpForgotPassword(req);

        return ResponseEntity.ok(
                ResponseUtil.success(
                        UUID.randomUUID().toString(),
                        new MessageResponse("Resend OTP successfully")
                )
        );
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/count")
    public ResponseEntity<Long> getUserCount() {
        long count = userService.countUsers();
        return ResponseEntity.ok(count);
    }


}

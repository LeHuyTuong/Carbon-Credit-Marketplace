package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.common.USER_ROLE;
import com.carbonx.marketcarbon.common.USER_STATUS;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.PasswordResetToken;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.TokenResponse;
import com.carbonx.marketcarbon.service.PasswordResetTokenService;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_INVALID);
        }
        String email = req.getEmail();
        String password = req.getPassword();
        String fullName = req.getFullName();
        USER_ROLE role = req.getRole();

        if (userRepository.findByEmail(email) != null) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setRole(role);
        newUser.setStatus(USER_STATUS.PENDING);

        // Sinh OTP
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        newUser.setOtpCode(otp);
        newUser.setOtpExpiredAt(java.time.OffsetDateTime.now().plusMinutes(5));
        userRepository.save(newUser);

        System.out.println("OTP for " + email + " = " + otp);
        // emailService.sendOtp(email, otp);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(null);
        authResponse.setMessage("Đăng ký thành công. OTP đã được gửi, vui lòng xác nhận.");
        authResponse.setRole(newUser.getRole());

        return ResponseEntity.ok(ResponseUtil.success("trace-register", authResponse));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) throw new AppException(ErrorCode.INVALID_OTP);

        if (user.getOtpCode() == null
                || !user.getOtpCode().equals(req.getOtpCode())
                || user.getOtpExpiredAt() == null
                || user.getOtpExpiredAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Xoá OTP, kích hoạt user
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        user.setStatus(USER_STATUS.ACTIVE);
        userRepository.save(user);

        // Sinh JWT
        String token = jwtProvider.generateToken(user);

        // Build AuthResponse (đồng bộ với login, register)
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("OTP verified successfully");
        authResponse.setRole(user.getRole());

        return ResponseEntity.ok(ResponseUtil.success("trace-verify-otp", authResponse));
    }


    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) throw new AppException(ErrorCode.EMAIL_INVALID);
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        if (user.getStatus() != USER_STATUS.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        );
        String token = jwtProvider.generateToken(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Login thành công");
        authResponse.setRole(user.getRole());

        return ResponseEntity.ok(ResponseUtil.success("trace-login", authResponse));
    }


    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<MessageResponse>> logout(
            @RequestHeader(name = "Authorization", required = false) String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(
                    ResponseUtil.error("trace-logout", "400", "Token không hợp lệ hoặc không được cung cấp")
            );
        }
        String token = bearer.substring(7);
        System.out.println("Logout token: " + token);

        return ResponseEntity.ok(ResponseUtil.success("trace-logout", new MessageResponse("Đăng xuất thành công")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CommonResponse<MessageResponse>> resetPassword(
            @RequestBody ResetPasswordRequest req) {

        PasswordResetToken resetToken = passwordResetTokenService.findByToken(req.getToken());
        if (resetToken == null) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (resetToken.isExpired()) {
            passwordResetTokenService.delete(resetToken);
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        User user = resetToken.getUser();
        userService.updatePassword(user, req.getPassword());
        passwordResetTokenService.delete(resetToken);

        MessageResponse payload = new MessageResponse("Password updated successfully");
        return ResponseEntity.ok(ResponseUtil.success("trace-reset-password", payload));
    }

    @PostMapping("/reset-password-request")
    public ResponseEntity<CommonResponse<MessageResponse>> resetPasswordRequest(@RequestParam("email") String email) {
        User user = userService.findUserByEmail(email);
        if (user == null) throw new AppException(ErrorCode.EMAIL_INVALID);
        userService.sendPasswordResetEmail(user);
        return ResponseEntity.ok(ResponseUtil.success("trace-reset-password-request", new MessageResponse("Password reset email sent successfully")));
    }

}

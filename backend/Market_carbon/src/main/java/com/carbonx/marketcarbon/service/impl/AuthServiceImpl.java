package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.OtpPurpose;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.PasswordResetToken;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.RoleRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.AuthService;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.PasswordResetTokenService;
import com.carbonx.marketcarbon.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_INVALID);
        }

        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User newUser = new User();
        newUser.setEmail(req.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        newUser.setStatus(USER_STATUS.PENDING);

        String requested = req.getRoleName().trim();

        Role role = roleRepository.findByName(requested)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        newUser.getRoles().add(role);
        // Sinh OTP
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        newUser.setOtpCode(otp);
        newUser.setOtpExpiryDate(LocalDateTime.now().plusMinutes(5));
        newUser.setOtpPurpose(OtpPurpose.REGISTER);
        userRepository.save(newUser);

        log.info("Generated OTP [{}] for user [{}] at {}", otp, newUser.getEmail(), LocalDateTime.now());

        String subject = "Xác thực tài khoản - CarbonX";
        String content = String.format(
                "<p>Xin chào %s,</p>" +
                        "<p>Cảm ơn bạn đã đăng ký. Vui lòng sử dụng mã OTP sau để xác nhận tài khoản:</p>" +
                        "<h2>%s</h2>" +
                        "<p>Mã OTP sẽ hết hạn sau 5 phút.</p>" +
                        "<p>Trân trọng,<br/>CarbonX Team</p>",
                newUser.getEmail(), otp
        );

        try {
            emailService.sendEmail(subject, content, List.of(req.getEmail()));
        } catch (Exception e) {
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(null);
        authResponse.setMessage("Registration successful. An OTP has been sent, please verify.");
        authResponse.setRoles(
                newUser.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );

        return authResponse;
    }

    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null || user.getOtpCode() == null
                || !user.getOtpCode().equals(req.getOtpCode())
                || user.getOtpExpiryDate() == null
                || user.getOtpExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Nếu OTP dùng cho quên mật khẩu → trả token tạm thay vì JWT login
        if (user.getOtpPurpose() == OtpPurpose.FORGOT_PASSWORD) {
            user.setOtpCode(null);
            user.setOtpExpiryDate(null);
            userRepository.save(user);

            String resetToken = jwtProvider.generateTemporaryToken(user, Duration.ofMinutes(10));
            AuthResponse response = new AuthResponse();
            response.setJwt(resetToken);
            response.setMessage("OTP verified for password reset");
            response.setRoles(Collections.emptyList());
            return response;
        }

        // Nếu OTP dùng cho đăng ký / đăng nhập → cấp JWT login như cũ
        user.setOtpCode(null);
        user.setOtpExpiryDate(null);
        user.setStatus(USER_STATUS.ACTIVE);
        userRepository.save(user);

        String jwt = jwtProvider.generateToken(user);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("OTP verified successfully");
        authResponse.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return authResponse;
    }


    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) throw new AppException(ErrorCode.EMAIL_INVALID);
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        if (user.getStatus() != USER_STATUS.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        String token = jwtProvider.generateToken(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Login successful");
        authResponse.setRoles(
                user.getRoles().stream()
                        .map(Role::getName)   // lấy tên role từ entity Role
                        .toList()
        );

        return authResponse;
    }

    @Override
    public MessageResponse logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return new MessageResponse("logout successful");
    }



}
package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.OtpPurpose;
import com.carbonx.marketcarbon.common.PredefinedRole;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.LoginRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.request.ResetPasswordRequest;
import com.carbonx.marketcarbon.dto.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.RoleRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.AuthService;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    // P0-A (N1): public self-registration may only create these roles.
    // ADMIN/CVA accounts must be provisioned through the admin flow, never self-registered.
    private static final Set<String> PUBLIC_REGISTRATION_ROLES = Set.of(
            PredefinedRole.USER_ROLE,
            PredefinedRole.COMPANY_ROLE
    );

    // Hạn OTP theo luồng: đăng ký 5 phút, quên mật khẩu 30 phút (khớp với
    // UserServiceImplementation). Dùng để suy ra thời điểm phát OTP.
    private static final int REGISTRATION_OTP_MINUTES = 5;
    private static final int FORGOT_PASSWORD_OTP_MINUTES = 30;

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
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

        // P0-A (N1): only self-service roles may be created through public registration.
        // ADMIN/CVA accounts must be provisioned through the admin flow, never self-registered.
        if (!PUBLIC_REGISTRATION_ROLES.contains(requested)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        newUser.getRoles().add(role);
        // Sinh OTP
        String otp = generateRegistrationOtp();
        newUser.setOtpCode(otp);
        newUser.setOtpExpiryDate(LocalDateTime.now().plusMinutes(5));
        newUser.setOtpPurpose(OtpPurpose.REGISTER);
        userRepository.save(newUser);

        // KHÔNG ghi OTP vào log: ai đọc được log (hay log collector) thì lấy được mã 6 số
        // và tự kích hoạt tài khoản trong 5 phút.
        log.info("Generated a registration OTP for user [{}] at {}", newUser.getEmail(), LocalDateTime.now());

        try {
            // OTP là email quan trọng: gửi đồng bộ để không bị pool thông báo drop
            // và để API không báo "đã gửi" khi SMTP thực sự thất bại.
            sendRegistrationOtp(newUser.getEmail(), otp);
        } catch (Exception e) {
            // @Transactional sẽ rollback row PENDING khi rời method bằng exception.
            // Delete tường minh là lớp bảo vệ thứ hai nếu method bị gọi ngoài proxy
            // (unit test, self-invocation), để không bao giờ để lại user không có OTP.
            try {
                userRepository.delete(newUser);
            } catch (Exception cleanupError) {
                log.error("Could not delete pending user [{}] after OTP failure: {}",
                        newUser.getEmail(), cleanupError.getMessage());
            }
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

    /**
     * Gửi lại OTP đăng ký cho user còn PENDING. Cần thiết cho các row PENDING đã bị
     * commit bởi phiên bản cũ khi SMTP lỗi (trước khi register có transaction), và cho
     * trường hợp email bị thất lạc trước khi hết hạn.
     */
    @Override
    @Transactional
    public AuthResponse resendRegistrationOtp(EmailRequest req) {
        AuthResponse response = new AuthResponse();
        response.setJwt(null);
        response.setMessage("A new registration OTP has been sent, please verify.");

        User user = userRepository.findByEmail(req.getEmail());
        if (user == null || user.getStatus() != USER_STATUS.PENDING) {
            // Trả cùng câu trả lời cho mọi email: endpoint này public, phân biệt "email có
            // tài khoản" và "không có" sẽ biến nó thành công cụ dò tài khoản. Người dùng mà
            // tài khoản đã bị xoá (OTP hết hạn) sẽ đăng ký lại được.
            log.info("Registration OTP resend requested for an address without a pending account");
            return response;
        }

        // Chống spam: chỉ chặn khi MÃ HIỆN TẠI vừa được phát (dưới 60 giây). Không
        // dùng hạn hết mã (5 phút) làm ngưỡng: người dùng bấm "Gửi lại" sau 60 giây
        // như UI cho phép sẽ bị từ chối tới 4 phút và không lấy được mã mới.
        if (user.getOtpPurpose() == OtpPurpose.REGISTER
                && otpIssuedLessThanSecondsAgo(user, 60)) {
            // Chỉ chặn khi mã ĐĂNG KÝ hiện tại vừa được phát. Nếu mã hiện tại là của
            // luồng quên mật khẩu (hạn 30 phút) thì người dùng không có mã đăng ký nào
            // để dùng, không có lý do gì chặn.
            throw new AppException(ErrorCode.OTP_STILL_VALID);
        }

        String otp = generateRegistrationOtp();
        user.setOtpCode(otp);
        user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(5));
        // Luôn đặt lại purpose REGISTER: tài khoản PENDING có thể đã bị luồng quên mật
        // khẩu ghi đè purpose thành FORGOT_PASSWORD, và verifyOtp với purpose đó chỉ
        // trả reset token chứ không kích hoạt tài khoản.
        user.setOtpPurpose(OtpPurpose.REGISTER);
        userRepository.save(user);

        try {
            sendRegistrationOtp(user.getEmail(), otp);
        } catch (Exception e) {
            // @Transactional rollback giữ nguyên OTP cũ; user có thể thử lại.
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }

        response.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return response;
    }

    private String generateRegistrationOtp() {
        return String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
    }

    /**
     * Mã OTP hiện tại có vừa được phát trong {@code seconds} giây gần đây không?
     *
     * <p>User không lưu thời điểm phát OTP, nhưng hạn mã là hằng số theo từng luồng
     * (đăng ký 5 phút, quên mật khẩu 30 phút) nên thời điểm phát được suy ra từ
     * {@code otpExpiryDate}.</p>
     */
    private boolean otpIssuedLessThanSecondsAgo(User user, int seconds) {
        if (user.getOtpExpiryDate() == null) {
            return false;
        }
        int lifetimeMinutes = user.getOtpPurpose() == OtpPurpose.FORGOT_PASSWORD
                ? FORGOT_PASSWORD_OTP_MINUTES
                : REGISTRATION_OTP_MINUTES;
        LocalDateTime issuedAt = user.getOtpExpiryDate().minusMinutes(lifetimeMinutes);
        return issuedAt.isAfter(LocalDateTime.now().minusSeconds(seconds));
    }

    private void sendRegistrationOtp(String email, String otp) throws Exception {
        String subject = "Account Verification - CarbonX";
        String content = String.format(
                "<p>Hello %s,</p>" +
                        "<p>Thank you for registering. Please use the following OTP code to verify your account:</p>" +
                        "<h2>%s</h2>" +
                        "<p>The OTP will expire in 5 minutes.</p>" +
                        "<p>Best regards,<br/>CarbonX Team</p>",
                email, otp
        );
        emailService.sendEmailNow(subject, content, List.of(email));
    }

    @Override
    @Transactional(dontRollbackOn = AppException.class)
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // Nếu OTP hết hạn
        if (user.getOtpExpiryDate() != null && user.getOtpExpiryDate().isBefore(LocalDateTime.now())) {
            // Nếu user chưa xác thực thì xóa luôn. dontRollbackOn = AppException.class
            // để lần xóa này được commit: nếu rollback theo exception thì row PENDING vẫn
            // còn và chặn người dùng đăng ký lại với EMAIL_EXISTED.
            if (user.getStatus() == USER_STATUS.PENDING) {
                userRepository.delete(user);
                log.warn(" OTP expired -> Deleted pending user [{}]", req.getEmail());
            }
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.getOtpCode())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        if (user.getOtpPurpose() == OtpPurpose.FORGOT_PASSWORD) {
            user.setOtpCode(null);
            user.setOtpExpiryDate(null);
            userRepository.save(user);

            String resetToken = jwtProvider.generateTemporaryToken(user, Duration.ofMinutes(10));
            AuthResponse response = new AuthResponse();
            response.setJwt(resetToken);
            response.setMessage("OTP verified for password reset");
            response.setRoles(Collections.emptyList());
            log.info(" OTP verified for password reset [{}]", req.getEmail());
            return response;
        }
        user.setOtpCode(null);
        user.setOtpExpiryDate(null);
        user.setStatus(USER_STATUS.ACTIVE);
        userRepository.save(user);

        String jwt = jwtProvider.generateToken(user);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("OTP verified successfully");
        authResponse.setRoles(user.getRoles().stream().map(Role::getName).toList());

        log.info(" OTP verified successfully for [{}]", req.getEmail());
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
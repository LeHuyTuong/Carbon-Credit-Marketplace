    package com.carbonx.marketcarbon.service.impl;

    import com.carbonx.marketcarbon.common.OtpPurpose;
    import com.carbonx.marketcarbon.config.JwtProvider;
    import com.carbonx.marketcarbon.dto.request.*;
    import com.carbonx.marketcarbon.dto.response.MessageResponse;
    import com.carbonx.marketcarbon.dto.response.UserResponse;
    import com.carbonx.marketcarbon.exception.AppException;
    import com.carbonx.marketcarbon.exception.ErrorCode;

    import com.carbonx.marketcarbon.model.PasswordResetToken;
    import com.carbonx.marketcarbon.model.User;
    import com.carbonx.marketcarbon.repository.PasswordResetTokenRepository;
    import com.carbonx.marketcarbon.repository.RoleRepository;
    import com.carbonx.marketcarbon.repository.UserRepository;
    import com.carbonx.marketcarbon.service.EmailService;
    import com.carbonx.marketcarbon.service.UserService;
    import jakarta.mail.MessagingException;
    import jakarta.transaction.Transactional;
    import lombok.AccessLevel;
    import lombok.RequiredArgsConstructor;
    import lombok.experimental.FieldDefaults;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.mail.SimpleMailMessage;
    import org.springframework.mail.javamail.JavaMailSender;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

    import java.io.UnsupportedEncodingException;
    import java.time.LocalDateTime;
    import java.util.*;

    @Service
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @Slf4j
    public class UserServiceImplementation implements UserService {

        private final UserRepository userRepository;
        private final JwtProvider jwtProvider;
        private final PasswordEncoder passwordEncoder;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final JavaMailSender javaMailSender;
        private final EmailService emailService;
        private final OtpService otpService;
        private final RoleRepository roleRepository;
//        private final UserMapper userMapper;


        @Override
        public User findUserProfileByJwt(String jwt) {
            String email = jwtProvider.getEmailFromJwtToken(jwt);

            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new AppException(ErrorCode.EMAIL_INVALID);
            }
            return user;
        }


        @Override
        public User findUserByEmail(String username) {
            User user = userRepository.findByEmail(username);
            if (user == null) {
                throw new AppException(ErrorCode.EMAIL_INVALID);
            }
            return user;
        }

        @Override
        public List<User> findALlUser() {
            return userRepository.findAll();
        }

        @Override
        public void updatePassword(User user, String newPassword) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }



        @Transactional
        public void sendOtpForgotPassword(EmailRequest request)
                throws MessagingException, UnsupportedEncodingException {

            User user = userRepository.findByEmail(request.getEmail());
            if (user == null) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }

            String otp = generateOtp();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

            user.setOtpCode(otp);
            user.setOtpExpiryDate(expiryDate);
            user.setOtpPurpose(OtpPurpose.FORGOT_PASSWORD);

            String subject = "Your OTP Code";
            String content = String.format(
                    "<p>Hello,</p>" +
                            "<p>We received a request to reset your password. Use the following OTP to reset it:</p>" +
                            "<h2>%s</h2>" +
                            "<p>If you did not request this, please ignore this email.</p>" +
                            "<p>Best regards,<br/>Your Company</p>",
                    otp
            );

            emailService.sendEmail(subject, content, List.of(user.getEmail()));
        }

        @Transactional
        public UserResponse createUser(UserCreationRequest request, String otp)  {
//            User existingUser = userRepository.findByEmail(request.getEmail());
//            if (existingUser != null) {
//                throw new AppException(ErrorCode.USER_EXISTED);
//            }
//
//            String storedOtp = otpService.getOtp(request.getEmail())
//                    .orElse(null);
//            if (storedOtp == null || !storedOtp.equals(otp)) {
//                throw new AppException(ErrorCode.INVALID_OTP);
//            }
//
//            User user = userMapper.toUser(request);
//
//            Role role = roleRepository.findByName(PredefinedRole.USER_ROLE)
//                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
//
//            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
//            user.setRole(role);
//            user.setFullName(request.getFirstName() + " " + request.getLastName());
//            userRepository.save(user);
//
//            otpService.deleteOtp(request.getEmail());
//
//
//            return userMapper.toUserResponse(user);
            return null;
        }

        private void sendEmail(String to, String subject, String message) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            javaMailSender.send(mailMessage);
        }
        private String generateRandomToken() {
            return UUID.randomUUID().toString();
        }
        private Date calculateExpiryDate() {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MINUTE, 10);
            return cal.getTime();
        }

        @Override
        @Transactional
        public MessageResponse resetPassword(ResetPasswordRequest req, String bearerToken) {
            //  Lấy email từ JWT reset token (header Authorization)
            String jwt = bearerToken.replace("Bearer ", "");
            String email = jwtProvider.getEmailFromJwtToken(jwt);

            User user = userRepository.findByEmail(email);
            if (user == null) throw new AppException(ErrorCode.USER_NOT_EXISTED);

            //  Kiểm tra confirmPassword
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new AppException(ErrorCode.CONFIRM_PASSWORD_INVALID);
            }

            //  Cập nhật mật khẩu
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            userRepository.save(user);

            return new MessageResponse("Password updated successfully");
        }

        private static String generateOtp(){

            Random random = new Random();
            StringBuilder otp = new StringBuilder();
            for(int i = 0; i < 6 ; i++){
                otp.append(random.nextInt(10));
            }
            return otp.toString();

        }
        @Transactional
        public void changePassword(String jwt, ChangePasswordRequest req) {
            // validate confirm
            if (!req.getNewPassword().equals(req.getConfirmPassword())) {
                throw new AppException(ErrorCode.CONFIRM_PASSWORD_INVALID);
            }

            // lấy email từ JWT
            String email = jwtProvider.getEmailFromJwtToken(jwt);
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }

            // kiểm tra mật khẩu cũ
            if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
            }

            // mã hoá và lưu mật khẩu mới
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
            userRepository.save(user);
        }

    }

package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.OtpPurpose;
import com.carbonx.marketcarbon.common.PredefinedRole;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.dto.request.EmailRequest;
import com.carbonx.marketcarbon.dto.request.RegisterRequest;
import com.carbonx.marketcarbon.dto.response.AuthResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.RoleRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.UserService;
import com.carbonx.marketcarbon.service.impl.AuthServiceImpl;
import com.carbonx.marketcarbon.config.JwtProvider;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-A (N1): public self-registration may only create EV_OWNER or COMPANY accounts.
 * ADMIN/CVA self-registration must be rejected; unknown roles keep the existing
 * ROLE_NOT_EXISTED (404) contract.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterRoleTest {

    @Mock private JwtProvider jwtProvider;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserService userService;
    @Mock private EmailService emailService;
    @Mock private RoleRepository roleRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(jwtProvider, userRepository, passwordEncoder,
                userService, emailService, roleRepository);
    }

    private RegisterRequest request(String role) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("Password@1");
        req.setConfirmPassword("Password@1");
        req.setRoleName(role);
        return req;
    }

    private void happyPathStubs(String roleName) {
        when(userRepository.findByEmail("user@example.com")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(Role.builder().name(roleName).build()));
    }

    @Test
    void register_evOwner_succeeds() throws Exception {
        happyPathStubs(PredefinedRole.USER_ROLE);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuthResponse res = authService.register(request(PredefinedRole.USER_ROLE));
        assertThat(res.getRoles()).containsExactly(PredefinedRole.USER_ROLE);
        verify(userRepository).save(any());
        // OTP is critical: it must bypass the droppable notification pool.
        verify(emailService).sendEmailNow(anyString(), anyString(), anyList());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyList());
    }

    @Test
    void register_otpSmtpFailureIsReportedInsteadOfSilentlySwallowed() throws Exception {
        happyPathStubs(PredefinedRole.USER_ROLE);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MessagingException("smtp down"))
                .when(emailService).sendEmailNow(anyString(), anyString(), anyList());

        assertThatThrownBy(() -> authService.register(request(PredefinedRole.USER_ROLE)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
        // The pending row must not survive a failed OTP send; otherwise the user is
        // stranded behind EMAIL_EXISTED with no registration-OTP resend path.
        verify(userRepository).delete(any());
    }

    @Test
    void resendRegistrationOtp_reissuesForPendingRegisterUser() throws Exception {
        User user = pendingUser();
        user.setOtpExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmailRequest req = new EmailRequest();
        req.setEmail("user@example.com");

        AuthResponse res = authService.resendRegistrationOtp(req);

        assertThat(res.getMessage()).contains("new registration OTP");
        assertThat(user.getOtpPurpose()).isEqualTo(OtpPurpose.REGISTER);
        assertThat(user.getOtpExpiryDate()).isAfter(LocalDateTime.now());
        verify(emailService).sendEmailNow(anyString(), anyString(), anyList());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyList());
    }

    @Test
    void resendRegistrationOtp_rejectsActiveUser() throws Exception {
        User user = pendingUser();
        user.setStatus(USER_STATUS.ACTIVE);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);

        EmailRequest req = new EmailRequest();
        req.setEmail("user@example.com");

        assertThatThrownBy(() -> authService.resendRegistrationOtp(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_EXISTED));
        verify(emailService, never()).sendEmailNow(anyString(), anyString(), anyList());
    }

    @Test
    void resendRegistrationOtp_rejectsStillValidOtp() {
        User user = pendingUser();
        user.setOtpExpiryDate(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);

        EmailRequest req = new EmailRequest();
        req.setEmail("user@example.com");

        assertThatThrownBy(() -> authService.resendRegistrationOtp(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.OTP_STILL_VALID));
    }

    @Test
    void resendRegistrationOtp_propagatesSmtpFailure() throws Exception {
        User user = pendingUser();
        user.setOtpExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MessagingException("smtp down"))
                .when(emailService).sendEmailNow(anyString(), anyString(), anyList());

        EmailRequest req = new EmailRequest();
        req.setEmail("user@example.com");

        assertThatThrownBy(() -> authService.resendRegistrationOtp(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }

    @Test
    void resendRegistrationOtp_worksWhenForgotPasswordOverwroteThePurpose() throws Exception {
        User user = pendingUser();
        // The public forgot-password flow overwrites otpPurpose on a PENDING account.
        // The rescue endpoint must still reissue a REGISTRATION code, otherwise the
        // account can never be activated (verifyOtp with FORGOT_PASSWORD only returns
        // a reset token).
        user.setOtpPurpose(OtpPurpose.FORGOT_PASSWORD);
        user.setOtpExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmailRequest req = new EmailRequest();
        req.setEmail("user@example.com");

        authService.resendRegistrationOtp(req);

        assertThat(user.getOtpPurpose()).isEqualTo(OtpPurpose.REGISTER);
        verify(emailService).sendEmailNow(anyString(), anyString(), anyList());
    }

    @Test
    void resendRegistrationOtp_doesNotRevealWhetherAnEmailExists() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(null);

        EmailRequest req = new EmailRequest();
        req.setEmail("nobody@example.com");

        assertThatThrownBy(() -> authService.resendRegistrationOtp(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_EXISTED));
    }

    private User pendingUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setStatus(USER_STATUS.PENDING);
        user.setOtpPurpose(OtpPurpose.REGISTER);
        return user;
    }

    @Test
    void register_company_succeeds() {
        happyPathStubs(PredefinedRole.COMPANY_ROLE);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuthResponse res = authService.register(request(PredefinedRole.COMPANY_ROLE));
        assertThat(res.getRoles()).containsExactly(PredefinedRole.COMPANY_ROLE);
    }

    @Test
    void register_admin_isRejected() throws Exception {
        happyPathStubs(PredefinedRole.ADMIN_ROLE);
        assertThatThrownBy(() -> authService.register(request(PredefinedRole.ADMIN_ROLE)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode().getStatusCode().value()).isEqualTo(403));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyList());
        verify(emailService, never()).sendEmailNow(anyString(), anyString(), anyList());
    }

    @Test
    void register_cva_isRejected() throws Exception {
        happyPathStubs(PredefinedRole.CVA_ROLE);
        assertThatThrownBy(() -> authService.register(request(PredefinedRole.CVA_ROLE)))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_unknownRole_keepsRoleNotExistedContract() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(null);
        when(roleRepository.findByName("SUPERUSER")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.register(request("SUPERUSER")))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.ROLE_NOT_EXISTED));
    }
}

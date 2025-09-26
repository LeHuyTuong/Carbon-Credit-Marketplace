package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.domain.USER_ROLE;
import com.carbonx.marketcarbon.domain.USER_STATUS;
import com.carbonx.marketcarbon.exception.UserException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.request.LoginRequest;
import com.carbonx.marketcarbon.request.RegisterRequest;
import com.carbonx.marketcarbon.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.response.AuthResponse;
import com.carbonx.marketcarbon.response.MessageResponse;
import com.carbonx.marketcarbon.response.TokenResponse;
import com.carbonx.marketcarbon.service.AuthService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ====================== REGISTER ======================
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) throws UserException {
        String email = req.getEmail();
        String password = req.getPassword();
        String fullName = req.getFullName();
        USER_ROLE role = req.getRole();

        if (userRepository.findByEmail(email) != null) {
            throw new UserException("Email đã tồn tại");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setRole(role);
        newUser.setStatus(USER_STATUS.PENDING); // chưa active, chờ OTP

        // Sinh OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
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

    // ====================== VERIFY OTP (sau REGISTER) ======================
    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) throws UserException {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) throw new UserException("Email không tồn tại");

        if (user.getOtpCode() == null
                || !user.getOtpCode().equals(req.getOtpCode())
                || user.getOtpExpiredAt() == null
                || user.getOtpExpiredAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new UserException("OTP không hợp lệ hoặc đã hết hạn");
        }

        // Xoá OTP, kích hoạt user
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        user.setStatus(USER_STATUS.ACTIVE);
        userRepository.save(user);

        // Sinh JWT
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        );
        String token = jwtProvider.generateToken(authentication);

        return ResponseEntity.ok(ResponseUtil.success("trace-verify-otp", new TokenResponse(token)));
    }

    // ====================== LOGIN (KHÔNG OTP) ======================
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) throws UserException {
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) throw new UserException("Email không tồn tại");
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UserException("Sai mật khẩu");
        }
        if (user.getStatus() != USER_STATUS.ACTIVE) {
            throw new UserException("Tài khoản chưa được kích hoạt OTP");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        );
        String token = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Login thành công");
        authResponse.setRole(user.getRole());

        return ResponseEntity.ok(ResponseUtil.success("trace-login", authResponse));
    }

    // ====================== LOGOUT ======================
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
}

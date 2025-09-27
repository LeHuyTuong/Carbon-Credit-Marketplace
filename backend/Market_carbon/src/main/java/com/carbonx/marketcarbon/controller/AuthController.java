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

    @Autowired private AuthService authService;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ====================== REGISTER ======================
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) throws UserException {
        String email = req.getEmail();
        String password = req.getPassword();
        String fullName = req.getFullName();
        USER_ROLE role = req.getRole();

        // Kiểm tra email đã tồn tại trong DB chưa
        if (userRepository.findByEmail(email) != null) {
            throw new UserException("Email Is Already Used With Another Account");
        }

        // Tạo đối tượng User mới
        User createdUser = new User();
        createdUser.setEmail(email);
        createdUser.setFullName(fullName);
        createdUser.setPasswordHash(passwordEncoder.encode(password)); // Mã hoá password
        createdUser.setRole(role);
        createdUser.setStatus(USER_STATUS.ACTIVE);

        // Lưu vào DB
        User savedUser = userRepository.save(createdUser);

        // Gán quyền (authorities) cho user
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.toString()));

        // Tạo Authentication object (đã xác thực)
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(email, password, authorities);

        // Đặt Authentication vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Sinh JWT token từ Authentication
        String token = jwtProvider.generateToken(authentication);

        // Chuẩn bị payload trả về
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);                  // JWT để client lưu
        authResponse.setMessage("Register Success"); // Thông báo cho client
        authResponse.setRole(savedUser.getRole());   // Role của user

        // Bọc response vào CommonResponse<AuthResponse>
        return ResponseEntity.ok(
                ResponseUtil.success("trace-register", authResponse)
        );
    }

    // ====================== LOGIN ======================
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) throws UserException {
        // Tìm user trong DB theo email
        User user = userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new UserException("Email không tồn tại");
        }

        // Kiểm tra password nhập có khớp với password hash trong DB
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UserException("Sai mật khẩu");
        }

        // Tạo Authentication object
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, new ArrayList<>());

        // Sinh JWT token
        String token = jwtProvider.generateToken(authentication);

        // Chuẩn bị payload trả về
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Đăng nhập thành công");
        authResponse.setRole(user.getRole());

        // Bọc response vào CommonResponse<AuthResponse>
        return ResponseEntity.ok(
                ResponseUtil.success("trace-login", authResponse)
        );
    }

    // ====================== VERIFY OTP ======================
    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        // Gọi service verify OTP
        TokenResponse tokenResponse = authService.verifyOtp(req);

        // Bọc response vào CommonResponse<TokenResponse>
        return ResponseEntity.ok(
                ResponseUtil.success("trace-verify-otp", tokenResponse)
        );
    }

    // ====================== LOGOUT ======================
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<MessageResponse>> logout(
            @RequestHeader(name = "Authorization", required = false) String bearer) {
        // Gọi service logout (thường là blacklist token)
        MessageResponse msg = authService.logout(bearer);

        // Bọc response vào CommonResponse<MessageResponse>
        return ResponseEntity.ok(
                ResponseUtil.success("trace-logout", msg)
        );
    }
}

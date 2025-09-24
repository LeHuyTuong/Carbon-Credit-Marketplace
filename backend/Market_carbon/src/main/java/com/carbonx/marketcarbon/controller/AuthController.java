package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.domain.USER_ROLE;
import com.carbonx.marketcarbon.exception.UserException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.request.LoginRequest;
import com.carbonx.marketcarbon.request.RegisterRequest;
import com.carbonx.marketcarbon.request.VerifyOtpRequest;
import com.carbonx.marketcarbon.response.ApiResponse;
import com.carbonx.marketcarbon.response.AuthResponse;
import com.carbonx.marketcarbon.response.MessageResponse;
import com.carbonx.marketcarbon.response.TokenResponse;
import com.carbonx.marketcarbon.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private JwtProvider jwtProvider;
    private final AuthService authService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody User user ) throws UserException {
        String email = user.getEmail();
        String password = user.getPasswordHash();
        String fullName = user.getFullName();
        USER_ROLE role = user.getRole();

        User isEmailExist = userRepository.findByEmail(email);

        if (isEmailExist!=null) {
            throw new UserException("Email Is Already Used With Another Account");
        }
        // Create new user
        User createdUser = new User();
        createdUser.setEmail(email);
        createdUser.setFullName(fullName);
        createdUser.setPasswordHash(passwordEncoder.encode(password));
        createdUser.setRole(role);

        User savedUser = userRepository.save(createdUser);

        // Tạo danh sách quyền (authorities) cho người dùng
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Thêm 1 quyền vào danh sách. SimpleGrantedAuthority nhận chuỗi tên quyền (ví dụ: "ROLE_USER" hoặc "ADMIN")
        // role.toString() cần trả về đúng format mà Security dùng để so khớp.
        authorities.add(new SimpleGrantedAuthority(role.toString()));

        // Tạo đối tượng Authentication “đã xác thực” từ username + credentials + authorities
        //  - principal: email (định danh người dùng)
        //  - credentials: password (có thể là plaintext ở thời điểm này; KHÔNG nên lưu lại sau khi dùng)
         //  - authorities: danh sách quyền
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password,authorities);
        // Đặt Authentication vào SecurityContext hiện tại để coi như user đã đăng nhập trong request này
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Sinh JWT từ Authentication. Triển khai jwtProvider.generateToken(authentication)
        // thường lấy username + roles và ký bằng secret key, trả về chuỗi token dạng Bearer.
        String token = jwtProvider.generateToken(authentication);
       // Dựng đối tượng phản hồi cho client
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);                 // JWT để client lưu và gửi kèm ở các request sau
        authResponse.setMessage("Register Success");// Thông điệp cho client
        authResponse.setRole(savedUser.getRole());  // Vai trò của user phục vụ UI/logic phía client

        // Trả về HTTP 200 cùng payload authResponse
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MessageResponse>> login(@Valid @RequestBody LoginRequest req){
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req){
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@RequestHeader(name="Authorization", required = false) String bearer){
        return ResponseEntity.ok(ApiResponse.ok(authService.logout(bearer)));
    }
}

package com.carbonx.marketcarbon.config;

import com.carbonx.marketcarbon.common.USER_ROLE;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {

    private final SecretKey key = Keys.hmacShaKeyFor(
            JwtConstant.SECRET_KEY.getBytes(StandardCharsets.UTF_8)
    );

    /**
     *  Tạo token đăng nhập (token chính)
     */

    public String generateToken(User user) {
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 ngày
                .claim("id", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Tạo token tạm thời (Temporary Token)
     * Dùng trong luồng Forgot Password sau khi OTP được xác minh.
     * Token này chỉ có thời hạn ngắn (ví dụ 10 phút) và không chứa roles.
     */

    public String generateTemporaryToken(User user, Duration validity) {
        long expireMs = validity.toMillis();
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .claim("email", user.getEmail())
                .claim("purpose", "RESET_PASSWORD") // giúp backend phân biệt loại token
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String email, USER_ROLE role) {
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 ngày
                .claim("email", email)
                .claim("roles", java.util.List.of(role.name())) // EV_OWNER từ enum
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 🔹 Giải mã token để lấy email người dùng
     */
    public String getEmailFromJwtToken(String jwt) {
        if (jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        return String.valueOf(claims.get("email"));
    }

    /**
     *  Giải mã token để lấy mục đích (purpose)
     */

    public String getPurposeFromJwt(String jwt) {
        if (jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        return String.valueOf(claims.get("purpose"));
    }

    /**
     * Gộp danh sách quyền thành chuỗi (phục vụ logging)
     */

    public String populateAuthorities(Collection<? extends GrantedAuthority> collection) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority authority : collection) {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }
}

package com.carbonx.marketcarbon.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtTokenValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy header Authorization
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);

            try {
                // Tạo SecretKey từ SECRET_KEY để verify HMAC-SHA
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

                // Parse + verify chữ ký + hạn token → lấy Claims
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();

                // Lấy email từ claim
                String email = String.valueOf(claims.get("email"));

                // Lấy danh sách quyền đã nhúng trong claim "roles"
                String roles = String.valueOf(claims.get("roles"));

                // Convert sang List<GrantedAuthority>
                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(roles);

                System.out.println("Token: " + email);
                System.out.println("Roles: " + roles);

                // Tạo Authentication đã xác thực
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(email, null, auths);

                // Đưa vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                throw new BadCredentialsException("invalid token...");
            }
        }

        filterChain.doFilter(request, response);
    }
}
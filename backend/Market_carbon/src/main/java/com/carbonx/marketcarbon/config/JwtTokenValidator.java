package com.carbonx.marketcarbon.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenValidator extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/verify-otp",
            "/api/v1/send-otp-forgot",
            "/api/v1/auth/send-otp-register",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/login",
            "/api/v1/send-otp-forgot"
            );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        //  1️ Bỏ qua các endpoint public, không check JWT
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        //  2️ Lấy token từ header
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        try {
            if (jwt != null && jwt.startsWith("Bearer ")) {
                jwt = jwt.substring(7);

                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();
        if (jwt == null || !jwt.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = jwt.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (rolesObj instanceof List<?>) {
                    for (Object role : (List<?>) rolesObj) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // tiếp tục các filter khác nếu không có lỗi
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, "Token expired");
        } catch (MalformedJwtException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, "Malformed token");
        } catch (UnsupportedJwtException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, "Unsupported token");
        } catch (IllegalArgumentException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, "Invalid token payload");
        } catch (Exception e) {
            handleException(response, HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }

    private void handleException(HttpServletResponse response,
                                 HttpStatus status,
                                 String message) throws IOException {
        // Ngắt luôn, không cho qua filter chain
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("""
            {
              "code": %d,
              "status": "%s",
              "message": "%s"
            }
            """, status.value(), status.name(), message));
    }
}

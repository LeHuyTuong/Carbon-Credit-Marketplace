package com.carbonx.marketcarbon.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            "/api/v1/send-otp-forgot",
            "/api/v1/check-exists-user",
            "/api/v1/projects/all",
            "/api/v1/forgot-password/resend-otp",
            "/oauth2/**",
            "/login/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        //  1️ Bỏ qua các endpoint public, không check JWT
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        //  2️ Lấy token từ header
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

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

            String email = String.valueOf(claims.get("email"));
            Object rolesObj = claims.get("roles");

            List<GrantedAuthority> auths = new ArrayList<>();
            if (rolesObj instanceof List<?>) {
                for (Object role : (List<?>) rolesObj) {
                    auths.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                }
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(email, null, auths);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            //  Debug log
            System.out.println("[JWT  OK] User: " + email + " | Roles: " + auths);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println(" JWT expired at: " + e.getClaims().getExpiration());
        } catch (Exception e) {
            System.err.println(" Invalid token: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

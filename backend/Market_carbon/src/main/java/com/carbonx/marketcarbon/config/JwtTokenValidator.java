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

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JwtTokenValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if (jwt != null && jwt.startsWith("Bearer ")) {
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
                        String roleName = String.valueOf(role);
                        auths.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }
                }
                // ✅ Debug: in ra authorities để bạn kiểm tra khi bị 403
                System.out.println("==== JwtTokenValidator ====");
                System.out.println("Email: " + email);
                auths.forEach(a -> System.out.println("Authority => " + a.getAuthority()));
                System.out.println("===========================");

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(email, null, auths);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                throw new BadCredentialsException("invalid token...");
            }
        }

        filterChain.doFilter(request, response);
    }
}
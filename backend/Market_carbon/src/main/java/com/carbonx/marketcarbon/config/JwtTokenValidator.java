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

        if(jwt!=null) {
            jwt=jwt.substring(7);


            try {
                // Tạo SecretKey từ SECRET_KEY để verify HMAC-SHA
                SecretKey key= Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

                // Parse + verify chữ ký + hạn token → lấy Claims
                Claims claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();

                // Lấy email từ claim tuỳ biến
                String email=String.valueOf(claims.get("email"));
                // Lấy danh sách quyền đã nhúng trong claim "authorities", dạng CSV: "ROLE_USER,ROLE_ADMIN"
                String authorities=String.valueOf(claims.get("authorities"));

                System.out.println("authorities -------- "+authorities);
                // Chuyển CSV → List<GrantedAuthority>
                List<GrantedAuthority> auths=AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                // Tạo Authentication đã xác thực với principal=email, credentials=null
                Authentication athentication=new UsernamePasswordAuthenticationToken(email,null, auths);
                // Đưa vào SecurityContext cho request hiện tại
                SecurityContextHolder.getContext().setAuthentication(athentication);

            } catch (Exception e) {
                throw new BadCredentialsException("invalid token...");
            }
        }
        filterChain.doFilter(request, response);

    }


}
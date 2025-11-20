package com.carbonx.marketcarbon.config;

import com.carbonx.marketcarbon.common.USER_ROLE;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class AppConfig {

    private final JwtTokenValidator jwtTokenValidator;
    private final JwtProvider jwtProvider;

    private static final String[] PUBLIC_ENDPOINT = {
            "/api/v1/auth/register",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/token",
            "/api/v1/auth/refresh",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/send-otp-register",
            "/api/v1/auth/check-exists-user",
            "/api/v1/auth/outbound/authentication",
            "/api/v1/send-otp-forgot",
            "/api/v1/check-exists-user",
            "/api/v1/projects/all",
            "/api/v1/forgot-password/resend-otp",
            "/api/v1/projects",               // Cho phép xem danh sách dự án
            "/api/v1/projects/{id}",          // Cho phép xem chi tiết dự án
            "/api/v1/reports/files/download", // Cho phép tải file/ảnh (Logo)
            "/api/v1/marketplace",            // Cho phép xem chợ tín chỉ
            "/files/**"
    };

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomOAuth2UserService customOAuth2UserService,
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository
    ) throws Exception {

        http
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resource").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().permitAll()
                )

                .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class)

                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(Customizer.withDefaults())

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.authorizationRequestRepository(cookieAuthorizationRequestRepository))
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler((req, res, auth) -> {
                            Object principal = auth.getPrincipal();
                            String email = null;

                            if (principal instanceof org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser oidcUser) {
                                email = oidcUser.getEmail();
                            } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User) {
                                email = (String) oauth2User.getAttributes().get("email");
                            }

                            if (email == null || email.isBlank()) {
                                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                res.setContentType("application/json");
                                res.getWriter().write("{\"error\":\"Email not found in Google profile\"}");
                                return;
                            }

                            String token = jwtProvider.generateToken(email, USER_ROLE.EV_OWNER);

                            String frontendUrl = req.getServerName().contains("localhost")
                                    ? "http://localhost:5173"
                                    : "https://carbonx.io.vn";

                            System.out.println("[OAuth2 SUCCESS] email=" + email + ", redirect=" + frontendUrl);
                            res.sendRedirect(frontendUrl + "/oauth-success?token=" + token);
                        })
                        .failureHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
                        })
                );

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:5173",
                    "http://127.0.0.1:*",
                    "http://192.168.*.*:*",
                    "https://carbonx.io.vn"
            ));
            cfg.setAllowedMethods(Collections.singletonList("*"));
            cfg.setAllowCredentials(true);
            cfg.setAllowedHeaders(Collections.singletonList("*"));
            cfg.setExposedHeaders(Arrays.asList("Authorization"));
            cfg.setMaxAge(3600L);
            return cfg;
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

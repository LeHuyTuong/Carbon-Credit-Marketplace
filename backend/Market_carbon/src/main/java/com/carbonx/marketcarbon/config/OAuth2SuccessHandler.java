//package com.carbonx.marketcarbon.config;
//
//import com.carbonx.marketcarbon.model.User;
//import com.carbonx.marketcarbon.service.AuthService;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
//
//    private final JwtProvider jwtProvider;
//    private final AuthService authService;
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest req,
//                                        HttpServletResponse res,
//                                        Authentication auth)
//            throws IOException, ServletException {
//
//        OAuth2User oAuth2User = (OAuth2User) auth.getPrincipal();
//
//        // 🟩 FIX Email lấy 100% từ attributes
//        String email = (String) oAuth2User.getAttributes().get("email");
//
//        if (email == null || email.isBlank()) {
//            res.setStatus(400);
//            res.setContentType("application/json");
//            res.getWriter().write("{\"error\":\"Email not found in Google profile\"}");
//            return;
//        }
//
//        // 🟩 Lấy user hoặc tạo mới với role mặc định EV_OWNER
//        User user = authService.processGoogleLogin(email);
//
//        // 🟩 Generate JWT token theo role của user
//        String token = jwtProvider.generateToken(user.getEmail(), user.getRoles());
//
//        // 🟩 Redirect về FE
//        String frontend = req.getServerName().contains("localhost")
//                ? "http://localhost:5173"
//                : "https://carbonx.io.vn";
//
//        res.sendRedirect(frontend + "/oauth-success?token=" + token);
//    }
//}
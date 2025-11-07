package com.carbonx.marketcarbon.config;

import com.carbonx.marketcarbon.common.USER_ROLE;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final JwtProvider jwtProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        log.info("Google OAuth attributes: {}", attributes);

        // Lấy email, fallback sang sub nếu email null
        String email = (String) attributes.get("email");
        if (email == null) {
            // một số account không cung cấp email → dùng sub (unique id)
            email = (String) attributes.get("sub");
            log.warn("Email not found in Google profile. Using sub={} as fallback identifier.", email);
        }

        String name = (String) attributes.getOrDefault("name", "Unknown");
        String token = jwtProvider.generateToken(email, USER_ROLE.EV_OWNER);

        return new OAuth2UserWithToken(oauth2User, token);
    }
}
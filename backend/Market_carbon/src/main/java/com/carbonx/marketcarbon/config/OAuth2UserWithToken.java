package com.carbonx.marketcarbon.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class OAuth2UserWithToken implements OAuth2User {

    private final OAuth2User delegate;
    private final String token;

    public OAuth2UserWithToken(OAuth2User delegate, String token) {
        this.delegate = delegate;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getAttribute("name");
    }
}

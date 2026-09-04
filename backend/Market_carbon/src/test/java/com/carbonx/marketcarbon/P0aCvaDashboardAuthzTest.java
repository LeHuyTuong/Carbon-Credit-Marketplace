package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.controller.CvaDashboardController;
import com.carbonx.marketcarbon.config.AppConfig;
import com.carbonx.marketcarbon.config.CustomOAuth2UserService;
import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.config.JwtTokenValidator;
import com.carbonx.marketcarbon.config.RateLimitInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-A authorization regression: invokes the (method-security-proxied) controller bean
 * directly with a controlled SecurityContext, exercising the real @PreAuthorize decisions.
 * (HTTP-level enforcement is covered by the P1 CI integration suite.)
 */
@WebMvcTest(com.carbonx.marketcarbon.controller.CvaDashboardController.class)
@Import(AppConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class P0aCvaDashboardAuthzTest {

    @Autowired private com.carbonx.marketcarbon.controller.CvaDashboardController controller;

    @MockBean private JwtTokenValidator jwtTokenValidator;
    @MockBean private JwtProvider jwtProvider;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private RateLimitInterceptor rateLimitInterceptor;
    @MockBean private com.carbonx.marketcarbon.service.DashboardCardService dashboardCardService;

    private void authenticate(String... roles) {
        String[] authorities = java.util.Arrays.stream(roles).map(r -> "ROLE_" + r).toArray(String[]::new);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tester@x.com", "n/a", authorities));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cva_canViewDashboard() {
        authenticate("CVA");
        assertThatCode(() -> controller.getAllCards(null, null)).doesNotThrowAnyException();
    }

    @Test
    void admin_canViewDashboard() {
        authenticate("ADMIN");
        assertThatCode(() -> controller.getAllCards(null, null)).doesNotThrowAnyException();
    }

    @Test
    void company_cannotViewDashboard() {
        authenticate("COMPANY");
        assertThatThrownBy(() -> controller.getAllCards(null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void evOwner_cannotViewDashboard() {
        authenticate("EV_OWNER");
        assertThatThrownBy(() -> controller.getAllCards(null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticated_cannotViewDashboard() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> controller.getAllCards(null, null))
                .isInstanceOf(Exception.class);
    }

}

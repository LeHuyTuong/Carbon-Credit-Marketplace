package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.controller.WithdrawalController;
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
@WebMvcTest(com.carbonx.marketcarbon.controller.WithdrawalController.class)
@Import(AppConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class P0aWithdrawalAuthzTest {

    @Autowired private com.carbonx.marketcarbon.controller.WithdrawalController controller;

    @MockBean private JwtTokenValidator jwtTokenValidator;
    @MockBean private JwtProvider jwtProvider;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private RateLimitInterceptor rateLimitInterceptor;
    @MockBean private com.carbonx.marketcarbon.service.WithdrawalService withdrawalService;
    @MockBean private com.carbonx.marketcarbon.service.WalletService walletService;
    @MockBean private com.carbonx.marketcarbon.service.WalletTransactionService walletTransactionService;

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
    void admin_canProcessWithdrawal() {
        authenticate("ADMIN");
        assertThatCode(() -> controller.processWithdrawal(1L, true, null, null)).doesNotThrowAnyException();
    }

    @Test
    void admin_canListAllWithdrawals() {
        authenticate("ADMIN");
        assertThatCode(() -> controller.getALlWithdrawalRequest(null, null)).doesNotThrowAnyException();
    }

    @Test
    void company_cannotProcessWithdrawal() {
        authenticate("COMPANY");
        assertThatThrownBy(() -> controller.processWithdrawal(1L, true, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void evOwner_cannotProcessWithdrawal() {
        authenticate("EV_OWNER");
        assertThatThrownBy(() -> controller.processWithdrawal(1L, false, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cva_cannotListWithdrawals() {
        authenticate("CVA");
        assertThatThrownBy(() -> controller.getALlWithdrawalRequest(null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticated_cannotProcessWithdrawal() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> controller.processWithdrawal(1L, true, null, null))
                .isInstanceOf(Exception.class);
    }

}

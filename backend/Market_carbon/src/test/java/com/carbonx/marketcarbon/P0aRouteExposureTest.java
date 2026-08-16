package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.AppConfig;
import com.carbonx.marketcarbon.config.CustomOAuth2UserService;
import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.config.JwtTokenValidator;
import com.carbonx.marketcarbon.config.RateLimitInterceptor;
import com.carbonx.marketcarbon.controller.ChatAiController;
import com.carbonx.marketcarbon.service.GeminiAiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-A (N10 + SC3): the /v1/ai duplicate route (rate-limiter bypass) is removed from
 * the request mappings, and the test upload controller class is deleted.
 */
@WebMvcTest(ChatAiController.class)
@Import(AppConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class P0aRouteExposureTest {

    @Autowired private RequestMappingHandlerMapping handlerMapping;

    @MockBean private JwtTokenValidator jwtTokenValidator;
    @MockBean private JwtProvider jwtProvider;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private RateLimitInterceptor rateLimitInterceptor;
    @MockBean private GeminiAiService geminiAiService;

    @Test
    void duplicatePublicAiRoute_isNotMapped() {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .noneMatch(info -> info.getDirectPaths().stream().anyMatch(p -> p.startsWith("/v1/ai"))
                        || info.getPatternValues().stream().anyMatch(p -> p.startsWith("/v1/ai")));
    }

    @Test
    void canonicalAiRoute_isStillMapped() {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .anyMatch(info -> info.getPatternValues().contains("/api/v1/ai/chat"));
    }

    @Test
    void fileUploadTestController_classDeleted() {
        // SC3: regression tripwire — the test upload controller must not come back
        assertThatThrownBy(() -> Class.forName("com.carbonx.marketcarbon.controller.FileUploadTestController"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}

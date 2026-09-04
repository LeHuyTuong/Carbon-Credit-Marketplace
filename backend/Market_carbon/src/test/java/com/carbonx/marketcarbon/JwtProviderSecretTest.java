package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.JwtProvider;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-A (N4): the signing secret comes from configuration. Tokens signed with a
 * different secret (i.e. after rotation) must be rejected. Uses generated test
 * secrets only — no real or previously-committed secret values.
 */
class JwtProviderSecretTest {

    private static final String SECRET_A = "a".repeat(64);
    private static final String SECRET_B = "b".repeat(64);

    private User sampleUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .roles(Set.of(Role.builder().name("COMPANY").build()))
                .build();
    }

    @Test
    void tokenRoundTrip_withInjectedSecret() {
        JwtProvider provider = new JwtProvider(SECRET_A);
        String token = provider.generateToken(sampleUser());
        assertThat(provider.getEmailFromJwtToken(token)).isEqualTo("user@example.com");
    }

    @Test
    void tokenSignedWithOldSecret_isRejectedAfterRotation() {
        String tokenFromOldSecret = new JwtProvider(SECRET_A).generateToken(sampleUser());
        JwtProvider rotatedProvider = new JwtProvider(SECRET_B);
        assertThatThrownBy(() -> rotatedProvider.getEmailFromJwtToken(tokenFromOldSecret))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void temporaryToken_carriesPurpose() {
        JwtProvider provider = new JwtProvider(SECRET_A);
        String token = provider.generateTemporaryToken(sampleUser(), java.time.Duration.ofMinutes(10));
        assertThat(provider.getPurposeFromJwt(token)).isEqualTo("RESET_PASSWORD");
    }
}

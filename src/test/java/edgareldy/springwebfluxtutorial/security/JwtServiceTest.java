package edgareldy.springwebfluxtutorial.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService: token generation, claim extraction, and validation of both
 * well-formed and tampered/malformed tokens.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-only-signing-secret-at-least-256-bits-long-0123456789", 3_600_000L);

    @Test
    void _01_ShouldRoundTripUsernameAndRole_WhenTokenIsGenerated() {
        String token = jwtService.generateToken("ada", "ADMIN");

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void _02_ShouldBeInvalid_WhenTokenIsTampered() {
        String token = jwtService.generateToken("ada", "USER");

        assertThat(jwtService.isValid(token + "tampered")).isFalse();
    }

    @Test
    void _03_ShouldBeInvalid_WhenTokenIsMalformed() {
        assertThat(jwtService.isValid("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void _04_ShouldBeInvalid_WhenTokenIsExpired() {
        JwtService shortLivedJwtService = new JwtService(
                "test-only-signing-secret-at-least-256-bits-long-0123456789", -1_000L);

        String alreadyExpiredToken = shortLivedJwtService.generateToken("ada", "USER");

        assertThat(shortLivedJwtService.isValid(alreadyExpiredToken)).isFalse();
    }
}

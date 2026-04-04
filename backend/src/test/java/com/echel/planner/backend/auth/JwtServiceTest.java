package com.echel.planner.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = "this-is-a-test-secret-that-is-at-least-32-chars";
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L;   // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        jwtService.validateKey();
    }

    @Test
    void generateAccessToken_extractEmail_roundtrip() {
        String email = "user@example.com";

        String token = jwtService.generateAccessToken(email);
        String extracted = jwtService.extractEmail(token);

        assertThat(extracted).isEqualTo(email);
    }

    @Test
    void generateRefreshToken_extractEmail_roundtrip() {
        String email = "refresh@example.com";

        String token = jwtService.generateRefreshToken(email);
        String extracted = jwtService.extractEmail(token);

        assertThat(extracted).isEqualTo(email);
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingUser() {
        String email = "valid@example.com";
        String token = jwtService.generateAccessToken(email);
        UserDetails userDetails = buildUserDetails(email);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forDifferentUser() {
        String token = jwtService.generateAccessToken("one@example.com");
        UserDetails userDetails = buildUserDetails("other@example.com");

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_throwsForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 0L);
        String email = "expired@example.com";
        String token = jwtService.generateAccessToken(email);
        UserDetails userDetails = buildUserDetails(email);

        // extractEmail (called inside isTokenValid) throws ExpiredJwtException
        // before the expiration check can return false
        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void validateKey_throwsIllegalStateException_forShortSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "tooshort");
        ReflectionTestUtils.setField(service, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        assertThatThrownBy(service::validateKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    private UserDetails buildUserDetails(String email) {
        return User.withUsername(email)
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
    }
}

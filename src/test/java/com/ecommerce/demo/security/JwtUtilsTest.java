package com.ecommerce.demo.security;

// CHANGE 10: Tests for JWT token generation and validation.

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Inject private @Value fields using ReflectionTestUtils (test-only utility)
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationMs", 604800000L);

        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("generateAccessToken: produces a non-null token")
    void generateAccessToken_notNull() {
        String token = jwtUtils.generateAccessToken(userDetails);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUsername: returns correct email from token")
    void extractUsername_correct() {
        String token = jwtUtils.generateAccessToken(userDetails);
        String extracted = jwtUtils.extractUsername(token);
        assertThat(extracted).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("isTokenValid: returns true for fresh token")
    void isTokenValid_true() {
        String token = jwtUtils.generateAccessToken(userDetails);
        assertThat(jwtUtils.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: returns false for token belonging to different user")
    void isTokenValid_wrongUser() {
        String token = jwtUtils.generateAccessToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "pass", Collections.emptyList());
        assertThat(jwtUtils.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken: is different from access token")
    void refreshToken_differFromAccessToken() {
        String access = jwtUtils.generateAccessToken(userDetails);
        String refresh = jwtUtils.generateRefreshToken(userDetails);
        assertThat(access).isNotEqualTo(refresh);
    }
}

package com.devvault.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final long EXPIRATION_MILLIS = 3600000; // 1 hour

    private String testSecret;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        testSecret = generateSecureTestSecret();
        jwtService = new JwtService(testSecret, EXPIRATION_MILLIS);
    }

    private String generateSecureTestSecret() {
        byte[] encodedKey = Jwts.SIG.HS256.key().build().getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

    @Test
    @DisplayName("Should generate a valid signed JWT with correct subject UUID and expiration")
    void generateToken_Success() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);

        Date expiration = jwtService.extractExpiration(token);
        assertThat(expiration).isAfter(new Date());

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
    }

    @Test
    @DisplayName("Should reject token when user ID is null")
    void generateToken_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Should reject expired tokens")
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // JwtService configured with negative expiration duration (-1000ms)
        JwtService expiredJwtService = new JwtService(testSecret, -1000);
        UUID userId = UUID.randomUUID();

        String token = expiredJwtService.generateToken(userId);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed tokens")
    void isTokenValid_MalformedToken_ReturnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.valid.jwt.token")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }

    @Test
    @DisplayName("Should reject tokens with an invalid signature")
    void isTokenValid_InvalidSignature_ReturnsFalse() {
        String differentSecret = generateSecureTestSecret();
        JwtService anotherJwtService = new JwtService(differentSecret, EXPIRATION_MILLIS);
        UUID userId = UUID.randomUUID();

        String tokenSignedWithDifferentSecret = anotherJwtService.generateToken(userId);

        assertThat(jwtService.isTokenValid(tokenSignedWithDifferentSecret)).isFalse();
    }
}

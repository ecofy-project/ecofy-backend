package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.TokenType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenTest {

    @Test
    void shouldCreateJwtTokenWithNormalizedValue() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        TokenType type = anyTokenType();

        JwtToken token = new JwtToken(
                "   header.payload.signature   ",
                expiresAt,
                type
        );

        assertEquals("header.payload.signature", token.value());
        assertEquals(expiresAt, token.expiresAt());
        assertEquals(type, token.type());
        assertFalse(token.isExpired());
        assertTrue(token.isActive());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken(
                        null,
                        Instant.now().plusSeconds(3600),
                        anyTokenType()
                )
        );

        assertEquals("value must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenValueIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtToken(
                        "   ",
                        Instant.now().plusSeconds(3600),
                        anyTokenType()
                )
        );

        assertEquals("JWT value must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenExpiresAtIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken(
                        "header.payload.signature",
                        null,
                        anyTokenType()
                )
        );

        assertEquals("expiresAt must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken(
                        "header.payload.signature",
                        Instant.now().plusSeconds(3600),
                        null
                )
        );

        assertEquals("type must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenExpiresAtIsEffectivelyInThePast() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtToken(
                        "header.payload.signature",
                        Instant.now().minusSeconds(10),
                        anyTokenType()
                )
        );

        assertEquals("expiresAt cannot be in the past", exception.getMessage());
    }

    @Test
    void shouldAllowTokenInsideClockSkewTolerance() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusSeconds(1),
                anyTokenType()
        );

        assertTrue(token.isExpired());
        assertFalse(token.isActive());
    }

    @Test
    void shouldReturnPositiveTimeToExpireForFutureToken() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                anyTokenType()
        );

        assertTrue(token.timeToExpire().toMillis() > 0);
    }

    @Test
    void shouldReturnTrueWhenTokenIsAboutToExpire() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(30),
                anyTokenType()
        );

        assertTrue(token.isAboutToExpire(Duration.ofMinutes(1)));
    }

    @Test
    void shouldReturnFalseWhenTokenIsNotAboutToExpire() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                anyTokenType()
        );

        assertFalse(token.isAboutToExpire(Duration.ofSeconds(1)));
    }

    @Test
    void shouldReturnFalseWhenExpiredTokenChecksAboutToExpire() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusSeconds(1),
                anyTokenType()
        );

        assertFalse(token.isAboutToExpire(Duration.ofMinutes(1)));
    }

    @Test
    void shouldThrowExceptionWhenThresholdIsNull() {
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                anyTokenType()
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> token.isAboutToExpire(null)
        );

        assertEquals("threshold must not be null", exception.getMessage());
    }

    @Test
    void shouldCompareEqualsByTokenValue() {
        TokenType type = anyTokenType();

        JwtToken token = new JwtToken(
                "same.jwt.value",
                Instant.now().plusSeconds(3600),
                type
        );

        JwtToken sameValue = new JwtToken(
                "same.jwt.value",
                Instant.now().plusSeconds(7200),
                type
        );

        JwtToken differentValue = new JwtToken(
                "different.jwt.value",
                Instant.now().plusSeconds(3600),
                type
        );

        assertEquals(token, token);
        assertEquals(token, sameValue);
        assertNotEquals(token, differentValue);
        assertNotEquals(token, null);
        assertNotEquals(token, "same.jwt.value");
    }

    @Test
    void shouldGenerateHashCodeUsingTokenValue() {
        TokenType type = anyTokenType();

        JwtToken token = new JwtToken(
                "same.jwt.value",
                Instant.now().plusSeconds(3600),
                type
        );

        JwtToken sameValue = new JwtToken(
                "same.jwt.value",
                Instant.now().plusSeconds(7200),
                type
        );

        assertEquals(token.hashCode(), sameValue.hashCode());
    }

    @Test
    void shouldReturnToStringWithMaskedLongTokenValue() {
        TokenType type = anyTokenType();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        JwtToken token = new JwtToken(
                "abcdefghijklmnop.jwt.signature",
                expiresAt,
                type
        );

        String result = token.toString();

        assertTrue(result.contains("JwtToken{"));
        assertTrue(result.contains("value='abcdefghijkl...'"));
        assertTrue(result.contains("expiresAt=" + expiresAt));
        assertTrue(result.contains("type=" + type));

        assertFalse(result.contains("abcdefghijklmnop.jwt.signature"));
    }

    @Test
    void shouldReturnToStringWithGenericMaskForShortTokenValue() {
        TokenType type = anyTokenType();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        JwtToken token = new JwtToken(
                "short.jwt",
                expiresAt,
                type
        );

        String result = token.toString();

        assertTrue(result.contains("JwtToken{"));
        assertTrue(result.contains("value='***'"));
        assertTrue(result.contains("expiresAt=" + expiresAt));
        assertTrue(result.contains("type=" + type));

        assertFalse(result.contains("short.jwt"));
    }

    private static TokenType anyTokenType() {
        return Arrays.stream(TokenType.values())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TokenType enum must have at least one value"));
    }
}
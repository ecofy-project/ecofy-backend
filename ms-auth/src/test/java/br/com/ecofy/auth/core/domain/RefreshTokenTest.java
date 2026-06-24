package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RefreshTokenTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T11:00:00Z");

    @Test
    void shouldCreateRefreshTokenWithAllFieldsAndNormalizeValues() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "   refresh-token-value   ",
                userId,
                "   ecofy-web   ",
                ISSUED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        assertEquals(ID, refreshToken.id());
        assertEquals("refresh-token-value", refreshToken.tokenValue());
        assertSame(userId, refreshToken.userId());
        assertEquals("ecofy-web", refreshToken.clientId());
        assertEquals(ISSUED_AT, refreshToken.issuedAt());
        assertEquals(EXPIRES_AT, refreshToken.expiresAt());
        assertFalse(refreshToken.isRevoked());
        assertEquals(TokenType.REFRESH, refreshToken.type());
    }

    @Test
    void shouldCreateRefreshTokenUsingFactory() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = RefreshToken.create(
                userId,
                "   ecofy-web   ",
                "   generated-refresh-token   ",
                3600
        );

        assertNotNull(refreshToken.id());
        assertEquals("generated-refresh-token", refreshToken.tokenValue());
        assertSame(userId, refreshToken.userId());
        assertEquals("ecofy-web", refreshToken.clientId());
        assertNotNull(refreshToken.issuedAt());
        assertNotNull(refreshToken.expiresAt());
        assertFalse(refreshToken.isRevoked());
        assertEquals(TokenType.REFRESH, refreshToken.type());
        assertTrue(refreshToken.expiresAt().isAfter(refreshToken.issuedAt()));
        assertTrue(refreshToken.isActive());
        assertFalse(refreshToken.isExpired());
    }

    @Test
    void shouldThrowExceptionWhenTtlSecondsIsZeroOrNegative() {
        AuthUserId userId = mock(AuthUserId.class);

        IllegalArgumentException zeroException = assertThrows(
                IllegalArgumentException.class,
                () -> RefreshToken.create(userId, "ecofy-web", "token", 0)
        );

        IllegalArgumentException negativeException = assertThrows(
                IllegalArgumentException.class,
                () -> RefreshToken.create(userId, "ecofy-web", "token", -1)
        );

        assertEquals("ttlSeconds must be greater than zero", zeroException.getMessage());
        assertEquals("ttlSeconds must be greater than zero", negativeException.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRequiredArgumentsAreNull() {
        AuthUserId userId = mock(AuthUserId.class);

        NullPointerException idException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        null,
                        "token",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException tokenValueException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        null,
                        userId,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException userIdException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        null,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException clientIdException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        null,
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException issuedAtException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "client-id",
                        null,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException expiresAtException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        null,
                        false,
                        TokenType.REFRESH
                )
        );

        NullPointerException typeException = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        null
                )
        );

        assertEquals("id must not be null", idException.getMessage());
        assertEquals("tokenValue must not be null", tokenValueException.getMessage());
        assertEquals("userId must not be null", userIdException.getMessage());
        assertEquals("clientId must not be null", clientIdException.getMessage());
        assertEquals("issuedAt must not be null", issuedAtException.getMessage());
        assertEquals("expiresAt must not be null", expiresAtException.getMessage());
        assertEquals("type must not be null", typeException.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTokenValueIsBlank() {
        AuthUserId userId = mock(AuthUserId.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        ID,
                        "   ",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        assertEquals("tokenValue must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClientIdIsBlank() {
        AuthUserId userId = mock(AuthUserId.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "   ",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        TokenType.REFRESH
                )
        );

        assertEquals("clientId must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotRefresh() {
        AuthUserId userId = mock(AuthUserId.class);
        TokenType nonRefreshType = nonRefreshTokenType();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        EXPIRES_AT,
                        false,
                        nonRefreshType
                )
        );

        assertEquals("RefreshToken.type must be REFRESH", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenExpiresAtIsBeforeIssuedAt() {
        AuthUserId userId = mock(AuthUserId.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        ID,
                        "token",
                        userId,
                        "client-id",
                        ISSUED_AT,
                        ISSUED_AT.minusSeconds(1),
                        false,
                        TokenType.REFRESH
                )
        );

        assertEquals("expiresAt must be greater than or equal to issuedAt", exception.getMessage());
    }

    @Test
    void shouldAllowExpiresAtEqualToIssuedAt() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                ISSUED_AT,
                ISSUED_AT,
                false,
                TokenType.REFRESH
        );

        assertEquals(ISSUED_AT, refreshToken.issuedAt());
        assertEquals(ISSUED_AT, refreshToken.expiresAt());
    }

    @Test
    void shouldReturnExpiredWhenExpiresAtIsInThePast() {
        AuthUserId userId = mock(AuthUserId.class);
        Instant issuedAt = Instant.now().minusSeconds(120);
        Instant expiresAt = Instant.now().minusSeconds(60);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                issuedAt,
                expiresAt,
                false,
                TokenType.REFRESH
        );

        assertTrue(refreshToken.isExpired());
        assertFalse(refreshToken.isActive());
    }

    @Test
    void shouldReturnActiveWhenNotRevokedAndNotExpired() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                false,
                TokenType.REFRESH
        );

        assertFalse(refreshToken.isExpired());
        assertTrue(refreshToken.isActive());
    }

    @Test
    void shouldReturnInactiveWhenRevokedEvenIfNotExpired() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                true,
                TokenType.REFRESH
        );

        assertTrue(refreshToken.isRevoked());
        assertFalse(refreshToken.isExpired());
        assertFalse(refreshToken.isActive());
    }

    @Test
    void shouldReturnPositiveTimeToExpireForFutureToken() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                false,
                TokenType.REFRESH
        );

        assertTrue(refreshToken.timeToExpire().toMillis() > 0);
    }

    @Test
    void shouldReturnNegativeTimeToExpireForExpiredToken() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token",
                userId,
                "client-id",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60),
                false,
                TokenType.REFRESH
        );

        assertTrue(refreshToken.timeToExpire().compareTo(Duration.ZERO) < 0);
    }

    @Test
    void shouldRevokeActiveToken() {
        RefreshToken refreshToken = activeToken();

        refreshToken.revoke();

        assertTrue(refreshToken.isRevoked());
        assertFalse(refreshToken.isActive());
    }

    @Test
    void shouldKeepRevokedTokenRevokedWhenRevokeIsCalledAgain() {
        RefreshToken refreshToken = activeToken();

        refreshToken.revoke();
        refreshToken.revoke();

        assertTrue(refreshToken.isRevoked());
        assertFalse(refreshToken.isActive());
    }

    @Test
    void shouldCompareRefreshTokensByIdOnly() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token-a",
                userId,
                "client-a",
                ISSUED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        RefreshToken sameId = new RefreshToken(
                ID,
                "token-b",
                userId,
                "client-b",
                ISSUED_AT.plusSeconds(10),
                EXPIRES_AT.plusSeconds(10),
                true,
                TokenType.REFRESH
        );

        RefreshToken differentId = new RefreshToken(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "token-a",
                userId,
                "client-a",
                ISSUED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        assertEquals(refreshToken, refreshToken);
        assertEquals(refreshToken, sameId);
        assertNotEquals(refreshToken, differentId);
        assertNotEquals(refreshToken, null);
        assertNotEquals(refreshToken, "token-a");
    }

    @Test
    void shouldGenerateHashCodeUsingIdOnly() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "token-a",
                userId,
                "client-a",
                ISSUED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        RefreshToken sameId = new RefreshToken(
                ID,
                "token-b",
                userId,
                "client-b",
                ISSUED_AT.plusSeconds(10),
                EXPIRES_AT.plusSeconds(10),
                true,
                TokenType.REFRESH
        );

        assertEquals(refreshToken.hashCode(), sameId.hashCode());
    }

    @Test
    void shouldReturnToStringWithMaskedLongTokenValue() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "abcdefghijklmnop-refresh-token",
                userId,
                "client-id",
                ISSUED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        String result = refreshToken.toString();

        assertTrue(result.contains("RefreshToken{"));
        assertTrue(result.contains("id=" + ID));
        assertTrue(result.contains("tokenValue='abcdefghijkl...'"));
        assertTrue(result.contains("clientId='client-id'"));
        assertTrue(result.contains("issuedAt=" + ISSUED_AT));
        assertTrue(result.contains("expiresAt=" + EXPIRES_AT));
        assertTrue(result.contains("revoked=false"));

        assertFalse(result.contains("abcdefghijklmnop-refresh-token"));
    }

    @Test
    void shouldReturnToStringWithGenericMaskForShortTokenValue() {
        AuthUserId userId = mock(AuthUserId.class);

        RefreshToken refreshToken = new RefreshToken(
                ID,
                "short-token",
                userId,
                "client-id",
                ISSUED_AT,
                EXPIRES_AT,
                true,
                TokenType.REFRESH
        );

        String result = refreshToken.toString();

        assertTrue(result.contains("RefreshToken{"));
        assertTrue(result.contains("tokenValue='***'"));
        assertTrue(result.contains("revoked=true"));

        assertFalse(result.contains("short-token"));
    }

    private RefreshToken activeToken() {
        return new RefreshToken(
                ID,
                "token",
                mock(AuthUserId.class),
                "client-id",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                false,
                TokenType.REFRESH
        );
    }

    private static TokenType nonRefreshTokenType() {
        return Arrays.stream(TokenType.values())
                .filter(type -> type != TokenType.REFRESH)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TokenType enum must have at least one non-REFRESH value"));
    }
}
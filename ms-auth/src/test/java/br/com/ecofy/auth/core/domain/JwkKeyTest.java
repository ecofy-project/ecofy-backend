package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwkKeyTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void shouldCreateJwkKeyWithAllFieldsAndNormalizeAlgorithmAndUse() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "-----BEGIN PUBLIC KEY-----FAKE-----END PUBLIC KEY-----",
                " rs256 ",
                " SIG ",
                CREATED_AT,
                true
        );

        assertEquals("kid-001", jwkKey.keyId());
        assertEquals("-----BEGIN PUBLIC KEY-----FAKE-----END PUBLIC KEY-----", jwkKey.publicKeyPem());
        assertEquals("RS256", jwkKey.algorithm());
        assertEquals(JwkKey.USE_SIGNING, jwkKey.use());
        assertEquals(CREATED_AT, jwkKey.createdAt());
        assertTrue(jwkKey.active());
        assertTrue(jwkKey.isSigningKey());
        assertFalse(jwkKey.isEncryptionKey());
    }

    @Test
    void shouldCreateEncryptionJwkKey() {
        JwkKey jwkKey = new JwkKey(
                "kid-enc-001",
                "public-key-pem",
                "rsa-oaep",
                " EnC ",
                CREATED_AT,
                false
        );

        assertEquals("RSA-OAEP", jwkKey.algorithm());
        assertEquals(JwkKey.USE_ENCRYPTION, jwkKey.use());
        assertFalse(jwkKey.active());
        assertFalse(jwkKey.isSigningKey());
        assertTrue(jwkKey.isEncryptionKey());
    }

    @Test
    void shouldUseSigningAsDefaultWhenUseIsNull() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "public-key-pem",
                "RS256",
                null,
                CREATED_AT,
                true
        );

        assertEquals(JwkKey.USE_SIGNING, jwkKey.use());
        assertTrue(jwkKey.isSigningKey());
        assertFalse(jwkKey.isEncryptionKey());
    }

    @Test
    void shouldUseSigningAsDefaultWhenUseIsBlank() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "public-key-pem",
                "RS256",
                "   ",
                CREATED_AT,
                true
        );

        assertEquals(JwkKey.USE_SIGNING, jwkKey.use());
        assertTrue(jwkKey.isSigningKey());
        assertFalse(jwkKey.isEncryptionKey());
    }

    @Test
    void shouldThrowExceptionWhenUseIsInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwkKey(
                        "kid-001",
                        "public-key-pem",
                        "RS256",
                        "invalid-use",
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "Invalid JWK use: invalid-use. Expected 'sig' or 'enc'.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenAlgorithmIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwkKey(
                        "kid-001",
                        "public-key-pem",
                        "   ",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        assertEquals("algorithm must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRequiredArgumentsAreNull() {
        NullPointerException keyIdException = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        null,
                        "public-key-pem",
                        "RS256",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        NullPointerException publicKeyPemException = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "kid-001",
                        null,
                        "RS256",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        NullPointerException algorithmException = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "kid-001",
                        "public-key-pem",
                        null,
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        NullPointerException createdAtException = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "kid-001",
                        "public-key-pem",
                        "RS256",
                        JwkKey.USE_SIGNING,
                        null,
                        true
                )
        );

        assertEquals("keyId must not be null", keyIdException.getMessage());
        assertEquals("publicKeyPem must not be null", publicKeyPemException.getMessage());
        assertEquals("algorithm must not be null", algorithmException.getMessage());
        assertEquals("createdAt must not be null", createdAtException.getMessage());
    }

    @Test
    void shouldCompareEqualsByKeyId() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "public-key-pem",
                "RS256",
                JwkKey.USE_SIGNING,
                CREATED_AT,
                true
        );

        JwkKey sameKeyId = new JwkKey(
                "kid-001",
                "another-public-key-pem",
                "ES256",
                JwkKey.USE_ENCRYPTION,
                CREATED_AT.plusSeconds(60),
                false
        );

        JwkKey differentKeyId = new JwkKey(
                "kid-002",
                "public-key-pem",
                "RS256",
                JwkKey.USE_SIGNING,
                CREATED_AT,
                true
        );

        assertEquals(jwkKey, jwkKey);
        assertEquals(jwkKey, sameKeyId);
        assertNotEquals(jwkKey, differentKeyId);
        assertNotEquals(jwkKey, null);
        assertNotEquals(jwkKey, "kid-001");
    }

    @Test
    void shouldGenerateHashCodeUsingKeyId() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "public-key-pem",
                "RS256",
                JwkKey.USE_SIGNING,
                CREATED_AT,
                true
        );

        JwkKey sameKeyId = new JwkKey(
                "kid-001",
                "another-public-key-pem",
                "ES256",
                JwkKey.USE_ENCRYPTION,
                CREATED_AT.plusSeconds(60),
                false
        );

        assertEquals(jwkKey.hashCode(), sameKeyId.hashCode());
    }

    @Test
    void shouldReturnToStringWithoutExposingPublicKeyPem() {
        JwkKey jwkKey = new JwkKey(
                "kid-001",
                "-----BEGIN PUBLIC KEY-----SECRET-PEM-----END PUBLIC KEY-----",
                "rs256",
                JwkKey.USE_SIGNING,
                CREATED_AT,
                true
        );

        String result = jwkKey.toString();

        assertTrue(result.contains("JwkKey{"));
        assertTrue(result.contains("keyId='kid-001'"));
        assertTrue(result.contains("algorithm='RS256'"));
        assertTrue(result.contains("use='sig'"));
        assertTrue(result.contains("active=true"));
        assertTrue(result.contains("createdAt=2026-01-01T10:00:00Z"));

        assertFalse(result.contains("SECRET-PEM"));
        assertFalse(result.contains("BEGIN PUBLIC KEY"));
        assertFalse(result.contains("END PUBLIC KEY"));
    }
}
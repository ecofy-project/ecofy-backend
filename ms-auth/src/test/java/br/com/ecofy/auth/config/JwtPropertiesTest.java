package br.com.ecofy.auth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    @Test
    void gettersAndSetters_shouldWork_andDefaultsShouldBeNonNegative() {
        JwtProperties p = new JwtProperties();

        assertNull(p.getIssuer());
        assertNull(p.getAudience());
        assertNull(p.getKeyId());
        assertNull(p.getPrivateKeyLocation());
        assertNull(p.getPublicKeyLocation());

        p.setIssuer("https://issuer.test");
        p.setAudience("aud-test");
        p.setKeyId("kid-1");
        p.setPrivateKeyLocation("classpath:keys/private.pem");
        p.setPublicKeyLocation("classpath:keys/public.pem");

        assertEquals("https://issuer.test", p.getIssuer());
        assertEquals("aud-test", p.getAudience());
        assertEquals("kid-1", p.getKeyId());
        assertEquals("classpath:keys/private.pem", p.getPrivateKeyLocation());
        assertEquals("classpath:keys/public.pem", p.getPublicKeyLocation());

        assertTrue(p.getAccessTokenTtlSeconds() > 0);
        assertTrue(p.getRefreshTokenTtlSeconds() > 0);
        assertTrue(p.getClockSkewSeconds() >= 0);
    }
}
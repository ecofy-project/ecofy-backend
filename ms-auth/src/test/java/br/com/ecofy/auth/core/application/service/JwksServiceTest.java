package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.JwkKey;
import br.com.ecofy.auth.core.port.out.JwksRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwksServiceTest {

    @Mock
    private JwksRepositoryPort jwksRepositoryPort;

    @Test
    void constructor_shouldRejectNullRepository() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new JwksService(null));
        assertEquals("jwksRepositoryPort must not be null", ex.getMessage());
    }

    @Test
    void getJwks_shouldThrowAuthException_whenNoActiveKeys() {
        JwksService service = new JwksService(jwksRepositoryPort);

        when(jwksRepositoryPort.findActiveSigningKeys()).thenReturn(List.of());

        AuthException ex = assertThrows(AuthException.class, service::getJwks);
        assertEquals(AuthErrorCode.JWKS_NOT_AVAILABLE, ex.getErrorCode());
        assertEquals("No active signing keys available", ex.getMessage());

        verify(jwksRepositoryPort).findActiveSigningKeys();
        verifyNoMoreInteractions(jwksRepositoryPort);
    }

    @Test
    void getJwks_shouldReturnKeysList_whenActiveKeysExist() {
        JwksService service = new JwksService(jwksRepositoryPort);

        JwkKey k1 = mock(JwkKey.class);
        when(k1.keyId()).thenReturn("kid-1");
        when(k1.algorithm()).thenReturn("RS256");
        when(k1.use()).thenReturn("sig");

        JwkKey k2 = mock(JwkKey.class);
        when(k2.keyId()).thenReturn("kid-2");
        when(k2.algorithm()).thenReturn("RS256");
        when(k2.use()).thenReturn("sig");

        when(jwksRepositoryPort.findActiveSigningKeys()).thenReturn(List.of(k1, k2));

        Map<String, Object> jwks = service.getJwks();

        assertNotNull(jwks);
        assertTrue(jwks.containsKey("keys"));

        Object keysObj = jwks.get("keys");
        assertInstanceOf(List.class, keysObj);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) keysObj;

        assertEquals(2, keys.size());

        assertEquals(Map.of("kid", "kid-1", "alg", "RS256", "use", "sig", "kty", "RSA"), keys.get(0));
        assertEquals(Map.of("kid", "kid-2", "alg", "RS256", "use", "sig", "kty", "RSA"), keys.get(1));

        verify(jwksRepositoryPort).findActiveSigningKeys();
        verifyNoMoreInteractions(jwksRepositoryPort);
    }

    @Test
    void convertToJwkEntry_shouldMapAllFields_andKeepInsertionOrder() throws Exception {
        JwksService service = new JwksService(jwksRepositoryPort);

        JwkKey key = mock(JwkKey.class);
        when(key.keyId()).thenReturn("kid-x");
        when(key.algorithm()).thenReturn("RS256");
        when(key.use()).thenReturn("sig");

        Method m = JwksService.class.getDeclaredMethod("convertToJwkEntry", JwkKey.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>) m.invoke(service, key);

        assertEquals("kid-x", entry.get("kid"));
        assertEquals("RS256", entry.get("alg"));
        assertEquals("sig", entry.get("use"));
        assertEquals("RSA", entry.get("kty"));

        String joinedKeys = String.join(",", entry.keySet());
        assertEquals("kid,alg,use,kty", joinedKeys);
    }
}
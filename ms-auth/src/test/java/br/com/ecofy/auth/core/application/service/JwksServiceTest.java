package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.out.PublicSigningKeyProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwksServiceTest {

    @Mock
    private PublicSigningKeyProviderPort publicSigningKeyProviderPort;

    @Test
    void constructor_shouldRejectNullProvider() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new JwksService(null));
        assertEquals("publicSigningKeyProviderPort must not be null", ex.getMessage());
    }

    @Test
    void getJwks_shouldThrowAuthException_whenNoActiveKeys() {
        JwksService service = new JwksService(publicSigningKeyProviderPort);

        when(publicSigningKeyProviderPort.currentPublicJwks()).thenReturn(List.of());

        AuthException ex = assertThrows(AuthException.class, service::getJwks);
        assertEquals(AuthErrorCode.JWKS_NOT_AVAILABLE, ex.getErrorCode());
        assertEquals("No active signing keys available", ex.getMessage());

        verify(publicSigningKeyProviderPort).currentPublicJwks();
        verifyNoMoreInteractions(publicSigningKeyProviderPort);
    }

    @Test
    void getJwks_shouldThrowAuthException_whenProviderReturnsNull() {
        JwksService service = new JwksService(publicSigningKeyProviderPort);

        when(publicSigningKeyProviderPort.currentPublicJwks()).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, service::getJwks);
        assertEquals(AuthErrorCode.JWKS_NOT_AVAILABLE, ex.getErrorCode());
    }

    @Test
    void getJwks_shouldExposeFullRsaPublicMaterial_includingModulusAndExponent() {
        JwksService service = new JwksService(publicSigningKeyProviderPort);

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("kid", "ecofy-auth-key-1");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n", "some-base64url-modulus");
        jwk.put("e", "AQAB");

        when(publicSigningKeyProviderPort.currentPublicJwks()).thenReturn(List.of(jwk));

        Map<String, Object> jwks = service.getJwks();

        assertNotNull(jwks);
        assertTrue(jwks.containsKey("keys"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertEquals(1, keys.size());

        Map<String, Object> entry = keys.get(0);
        // Material público RSA obrigatório para validação por Resource Servers:
        assertEquals("RSA", entry.get("kty"));
        assertEquals("ecofy-auth-key-1", entry.get("kid"));
        assertEquals("RS256", entry.get("alg"));
        assertEquals("sig", entry.get("use"));
        assertTrue(entry.containsKey("n"), "JWK deve conter o modulus 'n'");
        assertTrue(entry.containsKey("e"), "JWK deve conter o expoente 'e'");

        verify(publicSigningKeyProviderPort).currentPublicJwks();
        verifyNoMoreInteractions(publicSigningKeyProviderPort);
    }
}

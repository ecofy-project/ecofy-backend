package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceTest {

    @Mock
    private JwtTokenProviderPort jwtTokenProviderPort;

    @Test
    void constructor_shouldRejectNullProvider() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new TokenValidationService(null));
        assertEquals("jwtTokenProviderPort must not be null", ex.getMessage());
    }

    @Test
    void validate_shouldRejectNullToken() {
        TokenValidationService service = new TokenValidationService(jwtTokenProviderPort);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> service.validate(null));
        assertEquals("token must not be null", ex.getMessage());

        verifyNoInteractions(jwtTokenProviderPort);
    }

    @Test
    void validate_shouldThrowAuthException_whenSignatureOrExpiryInvalid() {
        TokenValidationService service = new TokenValidationService(jwtTokenProviderPort);

        // A validação REAL de assinatura/expiração lança IllegalArgumentException no provider.
        when(jwtTokenProviderPort.verifyAndParseClaims("bad"))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        AuthException ex = assertThrows(AuthException.class, () -> service.validate("bad"));
        assertEquals(AuthErrorCode.INVALID_TOKEN_SIGNATURE, ex.getErrorCode());
        assertEquals("Invalid token", ex.getMessage());

        verify(jwtTokenProviderPort).verifyAndParseClaims("bad");
        verifyNoMoreInteractions(jwtTokenProviderPort);
    }

    @Test
    void validate_shouldReturnClaims_whenTokenSignatureValid() {
        TokenValidationService service = new TokenValidationService(jwtTokenProviderPort);

        when(jwtTokenProviderPort.verifyAndParseClaims("good"))
                .thenReturn(Map.of("sub", "u1", "typ", "ACCESS"));

        Map<String, Object> claims = service.validate("good");

        assertEquals(Map.of("sub", "u1", "typ", "ACCESS"), claims);

        verify(jwtTokenProviderPort).verifyAndParseClaims("good");
        verifyNoMoreInteractions(jwtTokenProviderPort);
    }

    @Test
    void maskToken_shouldCoverAllBranches_inOneTest() throws Exception {
        TokenValidationService service = new TokenValidationService(jwtTokenProviderPort);

        Method m = TokenValidationService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);

        assertEquals("***", (String) m.invoke(service, (String) null));
        assertEquals("***", (String) m.invoke(service, "   "));
        assertEquals("***", (String) m.invoke(service, "123456789012"));
        assertEquals("123456789012...", (String) m.invoke(service, "1234567890123"));
    }
}
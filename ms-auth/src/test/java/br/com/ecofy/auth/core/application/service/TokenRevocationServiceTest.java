package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.in.RevokeTokenUseCase;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private RefreshTokenStorePort refreshTokenStorePort;

    @Test
    void constructor_shouldRejectNullStore() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new TokenRevocationService(null));
        assertEquals("refreshTokenStorePort must not be null", ex.getMessage());
    }

    @Test
    void revoke_shouldRejectNullCommand() {
        TokenRevocationService service = new TokenRevocationService(refreshTokenStorePort);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> service.revoke(null));
        assertEquals("command must not be null", ex.getMessage());

        verifyNoInteractions(refreshTokenStorePort);
    }

    @Test
    void revoke_shouldThrow_whenTokenTypeNotSupported() {
        TokenRevocationService service = new TokenRevocationService(refreshTokenStorePort);

        RevokeTokenUseCase.RevokeTokenCommand cmd = mock(RevokeTokenUseCase.RevokeTokenCommand.class);
        when(cmd.token()).thenReturn("tok-12345678901");
        when(cmd.refreshToken()).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> service.revoke(cmd));
        assertEquals(AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION, ex.getErrorCode());
        assertEquals("Only refresh tokens can be revoked", ex.getMessage());

        verify(cmd).token();
        verify(cmd, times(2)).refreshToken();
        verifyNoInteractions(refreshTokenStorePort);
    }

    @Test
    void revoke_shouldRevoke_whenRefreshTokenTrue() {
        TokenRevocationService service = new TokenRevocationService(refreshTokenStorePort);

        RevokeTokenUseCase.RevokeTokenCommand cmd = mock(RevokeTokenUseCase.RevokeTokenCommand.class);
        when(cmd.token()).thenReturn("refresh-token");
        when(cmd.refreshToken()).thenReturn(true);

        service.revoke(cmd);

        verify(refreshTokenStorePort).revoke("refresh-token");
        verifyNoMoreInteractions(refreshTokenStorePort);
    }

    @Test
    void maskToken_shouldCoverAllBranches_inOneTest() throws Exception {
        TokenRevocationService service = new TokenRevocationService(refreshTokenStorePort);

        Method m = TokenRevocationService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);

        assertEquals("***", (String) m.invoke(service, (String) null));
        assertEquals("***", (String) m.invoke(service, "   "));
        assertEquals("***", (String) m.invoke(service, "1234567890"));
        assertEquals("1234567890...", (String) m.invoke(service, "12345678901"));
    }
}
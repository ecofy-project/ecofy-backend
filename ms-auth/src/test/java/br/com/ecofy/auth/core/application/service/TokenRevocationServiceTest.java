package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.in.RevokeTokenUseCase;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de revogação de tokens")
class TokenRevocationServiceTest {

    @Mock
    private RefreshTokenStorePort refreshTokenStorePort;

    @Test
    @DisplayName("Deve rejeitar a dependência nula recebida pelo construtor")
    void constructor_dependenciaNula_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new TokenRevocationService(null)
        );

        assertEquals(
                "refreshTokenStorePort must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(refreshTokenStorePort);
    }

    @Test
    @DisplayName("Deve rejeitar o comando nulo sem acessar o armazenamento de tokens")
    void revoke_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        TokenRevocationService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.revoke(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(refreshTokenStorePort);
    }

    @Test
    @DisplayName("Deve rejeitar a revogação quando o token não for um refresh token")
    void revoke_tokenNaoRefresh_deveLancarAuthException() {
        // Arrange
        TokenRevocationService service = createService();
        String token = "access-token-with-more-than-ten-characters";

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        token,
                        false
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.revoke(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Only refresh tokens can be revoked",
                        exception.getMessage()
                )
        );

        verify(refreshTokenStorePort, never())
                .revoke(token);
    }

    @Test
    @DisplayName("Deve revogar o refresh token quando ele possuir mais de dez caracteres")
    void revoke_refreshTokenLongo_deveRevogarToken() {
        // Arrange
        TokenRevocationService service = createService();
        String token = "12345678901";

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        token,
                        true
                );

        // Act
        service.revoke(command);

        // Assert
        verify(refreshTokenStorePort)
                .revoke(token);
    }

    @Test
    @DisplayName("Deve revogar o refresh token quando ele possuir exatamente dez caracteres")
    void revoke_refreshTokenComDezCaracteres_deveRevogarToken() {
        // Arrange
        TokenRevocationService service = createService();
        String token = "1234567890";

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        token,
                        true
                );

        // Act
        service.revoke(command);

        // Assert
        verify(refreshTokenStorePort)
                .revoke(token);
    }

    @Test
    @DisplayName("Deve encaminhar o refresh token em branco para revogação")
    void revoke_refreshTokenEmBranco_deveEncaminharTokenParaRevogacao() {
        // Arrange
        TokenRevocationService service = createService();
        String token = "   ";

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        token,
                        true
                );

        // Act
        service.revoke(command);

        // Assert
        verify(refreshTokenStorePort)
                .revoke(token);
    }

    @Test
    @DisplayName("Deve encaminhar o refresh token nulo para revogação")
    void revoke_refreshTokenNulo_deveEncaminharTokenParaRevogacao() {
        // Arrange
        TokenRevocationService service = createService();

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        null,
                        true
                );

        // Act
        service.revoke(command);

        // Assert
        verify(refreshTokenStorePort)
                .revoke(null);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o armazenamento falhar ao revogar o refresh token")
    void revoke_falhaNoArmazenamento_devePropagarExcecao() {
        // Arrange
        TokenRevocationService service = createService();
        String token = "valid-refresh-token";

        RevokeTokenUseCase.RevokeTokenCommand command =
                new RevokeTokenUseCase.RevokeTokenCommand(
                        token,
                        true
                );

        IllegalStateException expectedException =
                new IllegalStateException("Token store unavailable");

        doThrow(expectedException)
                .when(refreshTokenStorePort)
                .revoke(token);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.revoke(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(refreshTokenStorePort)
                .revoke(token);
    }

    private TokenRevocationService createService() {
        return new TokenRevocationService(
                refreshTokenStorePort
        );
    }
}

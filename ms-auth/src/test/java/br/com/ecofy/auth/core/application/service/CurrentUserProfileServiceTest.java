package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.port.out.CurrentUserProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de perfil do usuário autenticado")
class CurrentUserProfileServiceTest {

    @Mock
    private CurrentUserProviderPort currentUserProviderPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser authUser;

    @Test
    @DisplayName("Deve rejeitar o provedor de usuário atual nulo")
    void constructor_provedorNulo_deveLancarNullPointerException() {
        // Arrange e Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new CurrentUserProfileService(null)
        );

        // Assert
        assertEquals(
                "currentUserProviderPort must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve retornar o usuário fornecido quando houver usuário autenticado")
    void getCurrentUser_usuarioAutenticado_deveRetornarUsuario() {
        // Arrange
        CurrentUserProfileService service = createService();

        when(currentUserProviderPort.getCurrentUserOrThrow())
                .thenReturn(authUser);

        // Act
        AuthUser result = service.getCurrentUser();

        // Assert
        assertSame(
                authUser,
                result
        );

        verify(currentUserProviderPort)
                .getCurrentUserOrThrow();
    }

    @Test
    @DisplayName("Deve converter falha do provedor em erro padronizado de autenticação")
    void getCurrentUser_provedorLancaExcecao_deveLancarAuthExceptionPadronizada() {
        // Arrange
        CurrentUserProfileService service = createService();

        IllegalStateException cause = new IllegalStateException(
                "Authentication context unavailable"
        );

        when(currentUserProviderPort.getCurrentUserOrThrow())
                .thenThrow(cause);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                service::getCurrentUser
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "No authenticated user found",
                        exception.getMessage()
                ),
                () -> assertSame(
                        cause,
                        exception.getCause()
                )
        );

        verify(currentUserProviderPort)
                .getCurrentUserOrThrow();

        verifyNoInteractions(authUser);
    }

    @Test
    @DisplayName("Deve converter retorno nulo do provedor em erro padronizado de autenticação")
    void getCurrentUser_provedorRetornaNulo_deveLancarAuthExceptionPadronizada() {
        // Arrange
        CurrentUserProfileService service = createService();

        when(currentUserProviderPort.getCurrentUserOrThrow())
                .thenReturn(null);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                service::getCurrentUser
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "No authenticated user found",
                        exception.getMessage()
                ),
                () -> assertInstanceOf(
                        NullPointerException.class,
                        exception.getCause()
                )
        );

        verify(currentUserProviderPort)
                .getCurrentUserOrThrow();

        verifyNoInteractions(authUser);
    }

    private CurrentUserProfileService createService() {
        return new CurrentUserProfileService(
                currentUserProviderPort
        );
    }
}

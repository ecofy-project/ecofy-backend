package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.port.in.ConfirmEmailUseCase;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.VerificationTokenStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de confirmação de e-mail")
class EmailConfirmationServiceTest {

    @Mock
    private VerificationTokenStorePort verificationTokenStorePort;

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser user;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser persistedUser;

    @Test
    @DisplayName("Deve rejeitar dependências nulas recebidas pelo construtor")
    void constructor_dependenciasNulas_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "verificationTokenStorePort must not be null",
                        () -> new EmailConfirmationService(
                                null,
                                saveAuthUserPort,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "saveAuthUserPort must not be null",
                        () -> new EmailConfirmationService(
                                verificationTokenStorePort,
                                null,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "publishAuthEventPort must not be null",
                        () -> new EmailConfirmationService(
                                verificationTokenStorePort,
                                saveAuthUserPort,
                                null
                        )
                )
        );

        verifyNoInteractions(
                verificationTokenStorePort,
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de confirmação nulo")
    void confirm_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        EmailConfirmationService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.confirm(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                verificationTokenStorePort,
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token de verificação nulo")
    void confirm_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        EmailConfirmationService service = createService();

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                createCommand(null);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.confirm(command)
        );

        // Assert
        assertEquals(
                "verificationToken must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                verificationTokenStorePort,
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token em branco quando ele não estiver armazenado")
    void confirm_tokenEmBrancoInexistente_deveLancarAuthException() {
        // Arrange
        EmailConfirmationService service = createService();
        String token = "   ";

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                createCommand(token);

        when(verificationTokenStorePort.consume(token))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.confirm(command)
        );

        // Assert
        assertInvalidTokenException(exception);

        verify(verificationTokenStorePort)
                .consume(token);

        verifyNoInteractions(
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token inválido com tamanho limite de dez caracteres")
    void confirm_tokenCurtoInvalido_deveLancarAuthException() {
        // Arrange
        EmailConfirmationService service = createService();
        String token = "1234567890";

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                createCommand(token);

        when(verificationTokenStorePort.consume(token))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.confirm(command)
        );

        // Assert
        assertInvalidTokenException(exception);

        verify(verificationTokenStorePort)
                .consume(token);

        verifyNoInteractions(
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve confirmar o e-mail, persistir o usuário e publicar o evento quando o token for válido")
    void confirm_tokenValido_deveConfirmarPersistirPublicarEventoERetornarUsuario() {
        // Arrange
        EmailConfirmationService service = createService();
        String token = "12345678901";

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                createCommand(token);

        when(verificationTokenStorePort.consume(token))
                .thenReturn(Optional.of(user));

        when(saveAuthUserPort.save(user))
                .thenReturn(persistedUser);

        // Act
        AuthUser result = service.confirm(command);

        // Assert
        assertSame(
                persistedUser,
                result
        );

        InOrder inOrder = inOrder(
                verificationTokenStorePort,
                user,
                saveAuthUserPort,
                publishAuthEventPort
        );

        inOrder.verify(verificationTokenStorePort)
                .consume(token);

        inOrder.verify(user)
                .confirmEmail();

        inOrder.verify(saveAuthUserPort)
                .save(user);

        inOrder.verify(publishAuthEventPort)
                .publish(any(UserEmailConfirmedEvent.class));
    }

    @Test
    @DisplayName("Deve propagar falha de persistência sem publicar o evento")
    void confirm_persistenciaFalha_devePropagarExcecaoSemPublicarEvento() {
        // Arrange
        EmailConfirmationService service = createService();
        String token = "valid-token-123";

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                createCommand(token);

        IllegalStateException expectedException =
                new IllegalStateException("Persistence unavailable");

        when(verificationTokenStorePort.consume(token))
                .thenReturn(Optional.of(user));

        when(saveAuthUserPort.save(user))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.confirm(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(user)
                .confirmEmail();

        verify(saveAuthUserPort)
                .save(user);

        verify(publishAuthEventPort, never())
                .publish(any(UserEmailConfirmedEvent.class));
    }

    private EmailConfirmationService createService() {
        return new EmailConfirmationService(
                verificationTokenStorePort,
                saveAuthUserPort,
                publishAuthEventPort
        );
    }

    private ConfirmEmailUseCase.ConfirmEmailCommand createCommand(
            String token
    ) {
        ConfirmEmailUseCase.ConfirmEmailCommand command =
                mock(ConfirmEmailUseCase.ConfirmEmailCommand.class);

        when(command.verificationToken())
                .thenReturn(token);

        return command;
    }

    private void assertInvalidTokenException(
            AuthException exception
    ) {
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Invalid or expired verification token",
                        exception.getMessage()
                )
        );
    }

    private void assertNullDependency(
            String expectedMessage,
            Executable executable
    ) {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                executable
        );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );
    }
}

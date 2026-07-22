package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RequestPasswordResetUseCase;
import br.com.ecofy.auth.core.port.in.ResetPasswordUseCase;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PasswordResetTokenStorePort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.SendResetPasswordEmailPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de redefinição de senha")
class PasswordResetServiceTest {

    private static final String USER_EMAIL = "user@ecofy.com";
    private static final String NEW_PASSWORD = "NewPassword@123";

    @Mock
    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;

    @Mock
    private PasswordResetTokenStorePort passwordResetTokenStorePort;

    @Mock
    private SendResetPasswordEmailPort sendResetPasswordEmailPort;

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser user;

    @Mock
    private PasswordHash newPasswordHash;

    @Test
    @DisplayName("Deve rejeitar dependências nulas recebidas pelo construtor")
    void constructor_dependenciasNulas_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "loadAuthUserByEmailPort must not be null",
                        () -> new PasswordResetService(
                                null,
                                passwordResetTokenStorePort,
                                sendResetPasswordEmailPort,
                                saveAuthUserPort,
                                passwordHashingPort,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "passwordResetTokenStorePort must not be null",
                        () -> new PasswordResetService(
                                loadAuthUserByEmailPort,
                                null,
                                sendResetPasswordEmailPort,
                                saveAuthUserPort,
                                passwordHashingPort,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "sendResetPasswordEmailPort must not be null",
                        () -> new PasswordResetService(
                                loadAuthUserByEmailPort,
                                passwordResetTokenStorePort,
                                null,
                                saveAuthUserPort,
                                passwordHashingPort,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "saveAuthUserPort must not be null",
                        () -> new PasswordResetService(
                                loadAuthUserByEmailPort,
                                passwordResetTokenStorePort,
                                sendResetPasswordEmailPort,
                                null,
                                passwordHashingPort,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "passwordHashingPort must not be null",
                        () -> new PasswordResetService(
                                loadAuthUserByEmailPort,
                                passwordResetTokenStorePort,
                                sendResetPasswordEmailPort,
                                saveAuthUserPort,
                                null,
                                publishAuthEventPort
                        )
                ),
                () -> assertNullDependency(
                        "publishAuthEventPort must not be null",
                        () -> new PasswordResetService(
                                loadAuthUserByEmailPort,
                                passwordResetTokenStorePort,
                                sendResetPasswordEmailPort,
                                saveAuthUserPort,
                                passwordHashingPort,
                                null
                        )
                )
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a solicitação de redefinição nula")
    void requestReset_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        PasswordResetService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.requestReset(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve finalizar sem gerar token quando o e-mail não estiver cadastrado")
    void requestReset_emailNaoCadastrado_deveFinalizarSemExporUsuario() {
        // Arrange
        PasswordResetService service = createService();

        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                createRequestCommand(USER_EMAIL);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.empty());

        ArgumentCaptor<EmailAddress> emailCaptor =
                ArgumentCaptor.forClass(EmailAddress.class);

        // Act
        service.requestReset(command);

        // Assert
        verify(loadAuthUserByEmailPort)
                .loadByEmail(emailCaptor.capture());

        assertEquals(
                USER_EMAIL,
                emailCaptor.getValue().value()
        );

        verifyNoInteractions(
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve gerar o token, armazená-lo, enviar o e-mail e publicar o evento para usuário cadastrado")
    void requestReset_emailCadastrado_deveArmazenarEnviarEPublicarEvento() {
        // Arrange
        PasswordResetService service = createService();

        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                createRequestCommand(USER_EMAIL);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.of(user));

        ArgumentCaptor<String> storedTokenCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> emailedTokenCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        PasswordResetRequestedEvent.class
                );

        // Act
        service.requestReset(command);

        // Assert
        InOrder inOrder = inOrder(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                publishAuthEventPort
        );

        inOrder.verify(loadAuthUserByEmailPort)
                .loadByEmail(any(EmailAddress.class));

        inOrder.verify(passwordResetTokenStorePort)
                .store(
                        same(user),
                        storedTokenCaptor.capture()
                );

        inOrder.verify(sendResetPasswordEmailPort)
                .sendReset(
                        same(user),
                        emailedTokenCaptor.capture()
                );

        inOrder.verify(publishAuthEventPort)
                .publish(eventCaptor.capture());

        String generatedToken = storedTokenCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        generatedToken,
                        emailedTokenCaptor.getValue()
                ),
                () -> assertDoesNotThrow(
                        () -> UUID.fromString(generatedToken)
                )
        );

        verifyNoInteractions(
                saveAuthUserPort,
                passwordHashingPort
        );
    }

    @Test
    @DisplayName("Deve propagar falha ao armazenar o token sem enviar e-mail ou publicar evento")
    void requestReset_armazenamentoFalha_devePropagarExcecaoSemContinuarProcessamento() {
        // Arrange
        PasswordResetService service = createService();

        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                createRequestCommand(USER_EMAIL);

        IllegalStateException expectedException =
                new IllegalStateException("Token store unavailable");

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.of(user));

        org.mockito.Mockito.doThrow(expectedException)
                .when(passwordResetTokenStorePort)
                .store(
                        any(AuthUser.class),
                        any(String.class)
                );

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.requestReset(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(sendResetPasswordEmailPort, never())
                .sendReset(
                        any(AuthUser.class),
                        any(String.class)
                );

        verify(publishAuthEventPort, never())
                .publish(any(PasswordResetRequestedEvent.class));
    }

    @Test
    @DisplayName("Deve rejeitar o comando de execução da redefinição nulo")
    void resetPassword_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        PasswordResetService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.resetPassword(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token nulo quando ele não identificar um usuário")
    void resetPassword_tokenNulo_deveLancarAuthException() {
        // Arrange
        PasswordResetService service = createService();

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(null);

        when(passwordResetTokenStorePort.consume(null))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(command)
        );

        // Assert
        assertInvalidResetTokenException(exception);

        verify(passwordResetTokenStorePort)
                .consume(null);

        verifyNoInteractions(
                passwordHashingPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token em branco quando ele não identificar um usuário")
    void resetPassword_tokenEmBranco_deveLancarAuthException() {
        // Arrange
        PasswordResetService service = createService();
        String token = "   ";

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(token);

        when(passwordResetTokenStorePort.consume(token))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(command)
        );

        // Assert
        assertInvalidResetTokenException(exception);

        verify(passwordResetTokenStorePort)
                .consume(token);

        verifyNoInteractions(
                passwordHashingPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token inválido com o limite de dez caracteres")
    void resetPassword_tokenComDezCaracteres_deveLancarAuthException() {
        // Arrange
        PasswordResetService service = createService();
        String token = "1234567890";

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(token);

        when(passwordResetTokenStorePort.consume(token))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(command)
        );

        // Assert
        assertInvalidResetTokenException(exception);

        verify(passwordResetTokenStorePort)
                .consume(token);

        verifyNoInteractions(
                passwordHashingPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar token inválido com mais de dez caracteres")
    void resetPassword_tokenComMaisDezCaracteres_deveLancarAuthException() {
        // Arrange
        PasswordResetService service = createService();
        String token = "12345678901";

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(token);

        when(passwordResetTokenStorePort.consume(token))
                .thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(command)
        );

        // Assert
        assertInvalidResetTokenException(exception);

        verify(passwordResetTokenStorePort)
                .consume(token);

        verifyNoInteractions(
                passwordHashingPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve alterar e persistir a nova senha quando o token for válido")
    void resetPassword_tokenValido_deveAlterarEPersistirNovaSenha() {
        // Arrange
        PasswordResetService service = createService();
        String token = "valid-reset-token";

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(token);

        when(command.newPassword())
                .thenReturn(NEW_PASSWORD);

        when(passwordResetTokenStorePort.consume(token))
                .thenReturn(Optional.of(user));

        when(passwordHashingPort.hash(NEW_PASSWORD))
                .thenReturn(newPasswordHash);

        // Act
        service.resetPassword(command);

        // Assert
        InOrder inOrder = inOrder(
                passwordResetTokenStorePort,
                passwordHashingPort,
                user,
                saveAuthUserPort
        );

        inOrder.verify(passwordResetTokenStorePort)
                .consume(token);

        inOrder.verify(passwordHashingPort)
                .hash(NEW_PASSWORD);

        inOrder.verify(user)
                .changePassword(newPasswordHash);

        inOrder.verify(saveAuthUserPort)
                .save(user);

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                sendResetPasswordEmailPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve propagar falha na geração do hash sem alterar ou persistir o usuário")
    void resetPassword_geracaoDoHashFalha_devePropagarExcecaoSemPersistirUsuario() {
        // Arrange
        PasswordResetService service = createService();
        String token = "valid-reset-token";

        ResetPasswordUseCase.ResetPasswordCommand command =
                createResetCommand(token);

        IllegalStateException expectedException =
                new IllegalStateException("Hashing unavailable");

        when(command.newPassword())
                .thenReturn(NEW_PASSWORD);

        when(passwordResetTokenStorePort.consume(token))
                .thenReturn(Optional.of(user));

        when(passwordHashingPort.hash(NEW_PASSWORD))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.resetPassword(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(user, never())
                .changePassword(any(PasswordHash.class));

        verify(saveAuthUserPort, never())
                .save(any(AuthUser.class));
    }

    private PasswordResetService createService() {
        return new PasswordResetService(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    private RequestPasswordResetUseCase.RequestPasswordResetCommand createRequestCommand(
            String email
    ) {
        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                mock(
                        RequestPasswordResetUseCase.RequestPasswordResetCommand.class
                );

        when(command.email())
                .thenReturn(email);

        return command;
    }

    private ResetPasswordUseCase.ResetPasswordCommand createResetCommand(
            String token
    ) {
        ResetPasswordUseCase.ResetPasswordCommand command =
                mock(ResetPasswordUseCase.ResetPasswordCommand.class);

        when(command.resetToken())
                .thenReturn(token);

        return command;
    }

    private void assertInvalidResetTokenException(
            AuthException exception
    ) {
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Invalid or expired reset token",
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

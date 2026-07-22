package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.dto.request.PasswordResetConfirmRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.PasswordResetRequest;
import br.com.ecofy.auth.core.port.in.RequestPasswordResetUseCase;
import br.com.ecofy.auth.core.port.in.ResetPasswordUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do controlador de redefinição de senha")
class PasswordControllerTest {

    @Mock
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    @Mock
    private ResetPasswordUseCase resetPasswordUseCase;

    private PasswordController controller;

    @BeforeEach
    void setUp() {
        controller = new PasswordController(
                requestPasswordResetUseCase,
                resetPasswordUseCase
        );
    }

    @Test
    @DisplayName("Deve aceitar a solicitação de redefinição e encaminhar o e-mail")
    void requestReset_requisicaoValida_deveRetornarStatusAccepted() {
        // Arrange
        PasswordResetRequest request =
                new PasswordResetRequest("user@ecofy.com");

        ArgumentCaptor<RequestPasswordResetUseCase.RequestPasswordResetCommand> captor =
                ArgumentCaptor.forClass(
                        RequestPasswordResetUseCase.RequestPasswordResetCommand.class
                );

        // Act
        ResponseEntity<Void> response =
                controller.requestReset(request);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());

        verify(requestPasswordResetUseCase)
                .requestReset(captor.capture());

        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                captor.getValue();

        assertEquals("user@ecofy.com", command.email());
        verifyNoInteractions(resetPasswordUseCase);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a solicitação de redefinição falhar")
    void requestReset_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        PasswordResetRequest request =
                new PasswordResetRequest("user@ecofy.com");

        IllegalStateException expectedException =
                new IllegalStateException("Falha ao solicitar redefinição");

        RequestPasswordResetUseCase.RequestPasswordResetCommand command =
                new RequestPasswordResetUseCase.RequestPasswordResetCommand(
                        request.email()
                );

        org.mockito.Mockito.doThrow(expectedException)
                .when(requestPasswordResetUseCase)
                .requestReset(command);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.requestReset(request)
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(requestPasswordResetUseCase).requestReset(command);
        verifyNoInteractions(resetPasswordUseCase);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a solicitação de redefinição for nula")
    void requestReset_requisicaoNula_deveLancarNullPointerException() {
        // Arrange
        PasswordResetRequest request = null;

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.requestReset(request)
        );

        // Assert
        verifyNoInteractions(
                requestPasswordResetUseCase,
                resetPasswordUseCase
        );
    }

    @Test
    @DisplayName("Deve confirmar a redefinição e encaminhar o token e a nova senha")
    void confirmReset_requisicaoValida_deveRetornarStatusNoContent() {
        // Arrange
        PasswordResetConfirmRequest request =
                new PasswordResetConfirmRequest(
                        "reset-token-123",
                        "NewStrongPassword123!"
                );

        ArgumentCaptor<ResetPasswordUseCase.ResetPasswordCommand> captor =
                ArgumentCaptor.forClass(
                        ResetPasswordUseCase.ResetPasswordCommand.class
                );

        // Act
        ResponseEntity<Void> response =
                controller.confirmReset(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(resetPasswordUseCase)
                .resetPassword(captor.capture());

        ResetPasswordUseCase.ResetPasswordCommand command =
                captor.getValue();

        assertEquals("reset-token-123", command.resetToken());
        assertEquals(
                "NewStrongPassword123!",
                command.newPassword()
        );
        verifyNoInteractions(requestPasswordResetUseCase);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a confirmação da redefinição falhar")
    void confirmReset_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        PasswordResetConfirmRequest request =
                new PasswordResetConfirmRequest(
                        "reset-token-123",
                        "NewStrongPassword123!"
                );

        IllegalStateException expectedException =
                new IllegalStateException("Falha ao redefinir senha");

        ResetPasswordUseCase.ResetPasswordCommand command =
                new ResetPasswordUseCase.ResetPasswordCommand(
                        request.token(),
                        request.newPassword()
                );

        org.mockito.Mockito.doThrow(expectedException)
                .when(resetPasswordUseCase)
                .resetPassword(command);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.confirmReset(request)
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(resetPasswordUseCase).resetPassword(command);
        verifyNoInteractions(requestPasswordResetUseCase);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a confirmação da redefinição for nula")
    void confirmReset_requisicaoNula_deveLancarNullPointerException() {
        // Arrange
        PasswordResetConfirmRequest request = null;

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.confirmReset(request)
        );

        // Assert
        verifyNoInteractions(
                requestPasswordResetUseCase,
                resetPasswordUseCase
        );
    }
}

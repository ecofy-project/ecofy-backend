package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.dto.request.ConfirmEmailRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RegisterUserRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.ConfirmEmailUseCase;
import br.com.ecofy.auth.core.port.in.RegisterUserUseCase;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do controlador de registro de usuários")
class RegistrationControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private ConfirmEmailUseCase confirmEmailUseCase;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new RegistrationController(
                registerUserUseCase,
                confirmEmailUseCase
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/register");

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve registrar o usuário com a localidade padrão quando ela não for informada")
    void register_localeNulo_deveUsarLocalidadePadrao() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest(
                "user@ecofy.com",
                "StrongPassword123!",
                "Matheus",
                "Lemes",
                null
        );

        AuthUser user = authUser(
                "user@ecofy.com",
                "Matheus",
                "Lemes",
                "pt-BR"
        );

        when(registerUserUseCase.register(any())).thenReturn(user);

        ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> captor =
                ArgumentCaptor.forClass(
                        RegisterUserUseCase.RegisterUserCommand.class
                );

        // Act
        ResponseEntity<UserResponse> response =
                controller.register(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        UserResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID.toString(), body.id());
        assertEquals("user@ecofy.com", body.email());
        assertEquals("Matheus Lemes", body.fullName());

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals("/api/user/me", location.getPath());

        verify(registerUserUseCase).register(captor.capture());

        RegisterUserUseCase.RegisterUserCommand command =
                captor.getValue();

        assertEquals("user@ecofy.com", command.email());
        assertEquals("StrongPassword123!", command.rawPassword());
        assertEquals("Matheus", command.firstName());
        assertEquals("Lemes", command.lastName());
        assertEquals("pt-BR", command.locale());
        assertFalse(command.autoConfirmEmail());
        assertEquals(List.of("ROLE_USER"), command.roles());

        verifyNoInteractions(confirmEmailUseCase);
    }

    @Test
    @DisplayName("Deve registrar o usuário com a localidade informada na requisição")
    void register_localeInformado_deveManterLocalidadeRecebida() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest(
                "user@ecofy.com",
                "StrongPassword123!",
                "Matheus",
                "Lemes",
                "en-US"
        );

        AuthUser user = authUser(
                "user@ecofy.com",
                "Matheus",
                "Lemes",
                "en-US"
        );

        when(registerUserUseCase.register(any())).thenReturn(user);

        ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> captor =
                ArgumentCaptor.forClass(
                        RegisterUserUseCase.RegisterUserCommand.class
                );

        // Act
        ResponseEntity<UserResponse> response =
                controller.register(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(registerUserUseCase).register(captor.capture());

        RegisterUserUseCase.RegisterUserCommand command =
                captor.getValue();

        assertEquals("user@ecofy.com", command.email());
        assertEquals("StrongPassword123!", command.rawPassword());
        assertEquals("Matheus", command.firstName());
        assertEquals("Lemes", command.lastName());
        assertEquals("en-US", command.locale());
        assertFalse(command.autoConfirmEmail());
        assertEquals(List.of("ROLE_USER"), command.roles());

        verifyNoInteractions(confirmEmailUseCase);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o registro do usuário falhar")
    void register_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest(
                "user@ecofy.com",
                "StrongPassword123!",
                "Matheus",
                "Lemes",
                "pt-BR"
        );

        IllegalStateException expectedException =
                new IllegalStateException("Falha ao registrar usuário");

        when(registerUserUseCase.register(any()))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.register(request)
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(registerUserUseCase).register(any());
        verifyNoInteractions(confirmEmailUseCase);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a requisição de registro for nula")
    void register_requisicaoNula_deveLancarNullPointerException() {
        // Arrange
        RegisterUserRequest request = null;

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.register(request)
        );

        // Assert
        verifyNoInteractions(
                registerUserUseCase,
                confirmEmailUseCase
        );
    }

    @Test
    @DisplayName("Deve confirmar o e-mail e retornar o usuário atualizado")
    void confirmEmail_tokenValido_deveRetornarUsuarioConfirmado() {
        // Arrange
        ConfirmEmailRequest request =
                new ConfirmEmailRequest("confirmation-token-123");

        AuthUser user = authUser(
                "user@ecofy.com",
                "Matheus",
                "Lemes",
                "pt-BR"
        );

        when(confirmEmailUseCase.confirm(any())).thenReturn(user);

        ArgumentCaptor<ConfirmEmailUseCase.ConfirmEmailCommand> captor =
                ArgumentCaptor.forClass(
                        ConfirmEmailUseCase.ConfirmEmailCommand.class
                );

        // Act
        ResponseEntity<UserResponse> response =
                controller.confirmEmail(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID.toString(), body.id());
        assertEquals("user@ecofy.com", body.email());
        assertEquals("Matheus Lemes", body.fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), body.status());

        verify(confirmEmailUseCase).confirm(captor.capture());

        ConfirmEmailUseCase.ConfirmEmailCommand command =
                captor.getValue();

        assertEquals(
                "confirmation-token-123",
                command.verificationToken()
        );

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a confirmação do e-mail falhar")
    void confirmEmail_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        ConfirmEmailRequest request =
                new ConfirmEmailRequest("invalid-token");

        IllegalStateException expectedException =
                new IllegalStateException("Falha ao confirmar e-mail");

        when(confirmEmailUseCase.confirm(any()))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.confirmEmail(request)
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(confirmEmailUseCase).confirm(any());
        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a requisição de confirmação for nula")
    void confirmEmail_requisicaoNula_deveLancarNullPointerException() {
        // Arrange
        ConfirmEmailRequest request = null;

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.confirmEmail(request)
        );

        // Assert
        verifyNoInteractions(
                registerUserUseCase,
                confirmEmailUseCase
        );
    }

    private AuthUser authUser(
            String email,
            String firstName,
            String lastName,
            String locale
    ) {
        return new AuthUser(
                new AuthUserId(USER_ID),
                new EmailAddress(email),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                firstName,
                lastName,
                locale,
                Set.of(
                        new Role(
                                "ROLE_USER",
                                "Usuário",
                                Set.of()
                        )
                ),
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                null,
                0
        );
    }
}

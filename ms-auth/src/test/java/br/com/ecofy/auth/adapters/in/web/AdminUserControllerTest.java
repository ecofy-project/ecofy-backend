package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.dto.request.AdminUserCreateRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
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
@DisplayName("Testes unitários do controlador administrativo de usuários")
class AdminUserControllerTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(registerUserUseCase);
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve criar o administrador com localidade e papéis padrão quando não forem informados")
    void createAdmin_localeERolesNulos_deveCriarUsuarioComValoresPadrao() {
        // Arrange
        setupRequestContext("/api/admin/users");

        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "admin@example.com",
                "StrongPass123!",
                "Admin",
                "User",
                null,
                null
        );

        UUID userId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );
        AuthUser domainUser = authUser(
                userId,
                "admin@example.com",
                "Admin",
                "User"
        );

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        RegisterUserUseCase.RegisterUserCommand.class
                );

        // Act
        ResponseEntity<UserResponse> response =
                controller.createAdmin(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId.toString(), response.getBody().id());
        assertEquals("admin@example.com", response.getBody().email());
        assertEquals("Admin User", response.getBody().fullName());

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith(
                "/api/admin/users/" + userId
        ));

        verify(registerUserUseCase).register(commandCaptor.capture());

        RegisterUserUseCase.RegisterUserCommand command =
                commandCaptor.getValue();

        assertEquals("admin@example.com", command.email());
        assertEquals("StrongPass123!", command.rawPassword());
        assertEquals("Admin", command.firstName());
        assertEquals("User", command.lastName());
        assertEquals("pt-BR", command.locale());
        assertNotNull(command.roles());
        assertFalse(command.roles().isEmpty());
    }

    @Test
    @DisplayName("Deve criar o administrador com a localidade e os papéis informados")
    void createAdmin_localeERolesInformados_deveCriarUsuarioComValoresRecebidos() {
        // Arrange
        setupRequestContext("/api/admin/users");

        List<String> customRoles = List.of(
                "AUTH_ADMIN",
                "AUTH_USER",
                "AUTH_AUDITOR"
        );

        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "root@example.com",
                "AnotherStrongPass!23",
                "Root",
                "User",
                "en-US",
                customRoles
        );

        UUID userId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );
        AuthUser domainUser = authUser(
                userId,
                "root@example.com",
                "Root",
                "User"
        );

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        RegisterUserUseCase.RegisterUserCommand.class
                );

        // Act
        ResponseEntity<UserResponse> response =
                controller.createAdmin(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId.toString(), response.getBody().id());
        assertEquals("root@example.com", response.getBody().email());
        assertEquals("Root User", response.getBody().fullName());

        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith(
                "/api/admin/users/" + userId
        ));

        verify(registerUserUseCase).register(commandCaptor.capture());

        RegisterUserUseCase.RegisterUserCommand command =
                commandCaptor.getValue();

        assertEquals("root@example.com", command.email());
        assertEquals("AnotherStrongPass!23", command.rawPassword());
        assertEquals("Root", command.firstName());
        assertEquals("User", command.lastName());
        assertEquals("en-US", command.locale());
        assertEquals(customRoles, command.roles());
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o registro do administrador falhar")
    void createAdmin_falhaNoRegistro_devePropagarExcecao() {
        // Arrange
        setupRequestContext("/api/admin/users");

        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "admin@example.com",
                "StrongPass123!",
                "Admin",
                "User",
                "pt-BR",
                List.of("ROLE_ADMIN")
        );

        IllegalStateException expectedException =
                new IllegalStateException("Falha ao registrar usuário");

        when(registerUserUseCase.register(any()))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.createAdmin(request)
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(registerUserUseCase).register(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando a requisição for nula")
    void createAdmin_requestNulo_deveLancarNullPointerException() {
        // Arrange
        AdminUserCreateRequest request = null;

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.createAdmin(request)
        );

        // Assert
        verifyNoInteractions(registerUserUseCase);
    }

    private void setupRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

    private AuthUser authUser(
            UUID id,
            String email,
            String firstName,
            String lastName
    ) {
        return new AuthUser(
                new AuthUserId(id),
                new EmailAddress(email),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                firstName,
                lastName,
                "pt-BR",
                Set.of(new Role("ROLE_ADMIN", "Administrador", Set.of())),
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                null,
                0
        );
    }
}
